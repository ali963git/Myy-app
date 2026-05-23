package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.ThakiroonViewModel
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThakiroonMainScreen(
    viewModel: ThakiroonViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Observe flows from ViewModel
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarksList.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val selectedArticle by viewModel.selectedArticle.collectAsStateWithLifecycle()
    val selectedVideo by viewModel.selectedVideo.collectAsStateWithLifecycle()

    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()

    // Immersive Reading Modal
    selectedArticle?.let { article ->
        ArticleReaderDialog(
            article = article,
            isBookmarked = bookmarkedIds.contains(article.id),
            onToggleBookmark = {
                viewModel.toggleBookmark(
                    article.id,
                    "article",
                    article.title,
                    article.author
                )
            },
            onDismiss = { viewModel.openArticle(null) }
        )
    }

    // Immersive Simulated Video Player Modal
    selectedVideo?.let { video ->
        SimulatedVideoPlayerDialog(
            video = video,
            isBookmarked = bookmarkedIds.contains(video.id),
            onToggleBookmark = {
                viewModel.toggleBookmark(
                    video.id,
                    "video",
                    video.title,
                    video.durationText
                )
            },
            onDismiss = { viewModel.openVideo(null) }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp

        Scaffold(
            topBar = {
                ThakiroonTopAppBar(
                    listState = listState,
                    scope = coroutineScope,
                    searchQuery = searchQuery,
                    onSearchChanged = { viewModel.updateSearchQuery(it) }
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkBg,
                                SurfaceOlive,
                                DarkBg
                            )
                        )
                    )
            ) {
                // Calm, glowing particles floating slowly in the background
                AmbientSpiritualParticles(modifier = Modifier.fillMaxSize())

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 48.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(40.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = if (isWide) 960.dp else 2000.dp)
                        .align(Alignment.TopCenter)
                ) {
                    // 1. Hero Banner
                    item {
                        HeroBannerSection(
                            isWide = isWide,
                            onListenQuranClicked = {
                                // Smooth scroll to Audio Player (Index 3)
                                coroutineScope.launch {
                                    listState.animateScrollToItem(3)
                                }
                                // Start playing first track
                                viewModel.selectTrack(StaticContent.audioTracksList.first())
                            },
                            onBrowseContentClicked = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(1)
                                }
                            }
                        )
                    }

                    // 2. Categories selector
                    item {
                        CategoriesSection(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { category ->
                                viewModel.selectCategory(category)
                            }
                        )
                    }

                    // 3. Featured Articles Group
                    item {
                        FeaturedArticlesSection(
                            selectedCategory = selectedCategory,
                            bookmarkedIds = bookmarkedIds,
                            onArticleClick = { article ->
                                viewModel.openArticle(article)
                            },
                            onBookmarkClick = { article ->
                                viewModel.toggleBookmark(
                                    article.id,
                                    "article",
                                    article.title,
                                    article.author
                                )
                            }
                        )
                    }

                    // 4. Audio Panel
                    item {
                        AudioPanelSection(
                            isWide = isWide,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            progress = playbackProgress,
                            elapsedSeconds = elapsedSeconds,
                            bookmarkedIds = bookmarkedIds,
                            onPlayPauseToggle = { viewModel.togglePlayPause() },
                            onNext = { viewModel.nextTrack() },
                            onPrev = { viewModel.previousTrack() },
                            onSeek = { viewModel.seekTo(it) },
                            onTrackSelect = { track -> viewModel.selectTrack(track) },
                            onBookmarkToggle = { track ->
                                viewModel.toggleBookmark(
                                    track.id,
                                    "audio",
                                    track.title,
                                    track.reciter
                                )
                            }
                        )
                    }

                    // 5. Video Section
                    item {
                        FeaturedVideosSection(
                            isWide = isWide,
                            bookmarkedIds = bookmarkedIds,
                            onVideoClick = { video ->
                                viewModel.openVideo(video)
                            },
                            onBookmarkToggle = { video ->
                                viewModel.toggleBookmark(
                                    video.id,
                                    "video",
                                    video.title,
                                    video.durationText
                                )
                            }
                        )
                    }

                    // 6. Interactive Bookmarks Shelf
                    if (bookmarks.isNotEmpty()) {
                        item {
                            BookmarksShelfSection(
                                bookmarks = bookmarks,
                                onItemClick = { bookmark ->
                                    when (bookmark.type) {
                                        "article" -> {
                                            val art = StaticContent.articlesList.find { it.id == bookmark.id }
                                            if (art != null) viewModel.openArticle(art)
                                        }
                                        "video" -> {
                                            val vid = StaticContent.videosList.find { it.id == bookmark.id }
                                            if (vid != null) viewModel.openVideo(vid)
                                        }
                                        "audio" -> {
                                            val aud = StaticContent.audioTracksList.find { it.id == bookmark.id }
                                            if (aud != null) viewModel.selectTrack(aud)
                                        }
                                    }
                                },
                                onRemoveBookmark = { id ->
                                    viewModel.toggleBookmark(id, "", "", "")
                                }
                            )
                        }
                    }

                    // 7. Footer
                    item {
                        FooterSection(
                            onLinkClick = { section ->
                                coroutineScope.launch {
                                    when (section) {
                                        "الرئيسية" -> listState.animateScrollToItem(0)
                                        "المقالات" -> listState.animateScrollToItem(2)
                                        "الصوتيات" -> listState.animateScrollToItem(3)
                                        "المرئيات" -> listState.animateScrollToItem(4)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TOP APP BAR
// ==========================================
@Composable
fun ThakiroonTopAppBar(
    listState: LazyListState,
    scope: CoroutineScope,
    searchQuery: String,
    onSearchChanged: (String) -> Unit
) {
    var searchActive by remember { mutableStateOf(false) }

    Surface(
        color = SurfaceOlive,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(Color.Transparent, BorderGreen)),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (searchActive) {
                // Expanded Search Input
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = {
                        Text(
                            text = "ابحث عن مقال أو صوتيات...",
                            color = TextSage,
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedIndicatorColor = GoldAccent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .height(48.dp)
                        .testTag("search_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = GoldAccent
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            searchActive = false
                            onSearchChanged("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close search",
                                tint = TextSage
                            )
                        }
                    }
                )
            } else {
                // Traditional Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LogoPattern(modifier = Modifier.size(28.dp))
                    Text(
                        text = "ذاكرون",
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.testTag("nav_logo_text")
                    )
                }

                // Inline Navigation tabs for screen shortcuts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    val items = listOf("الرئيسية", "المقالات", "الصوتيات", "المرئيات")
                    items.forEachIndexed { index, name ->
                        Box(
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        when (index) {
                                            0 -> listState.animateScrollToItem(0)
                                            1 -> listState.animateScrollToItem(2)
                                            2 -> listState.animateScrollToItem(3)
                                            3 -> listState.animateScrollToItem(4)
                                        }
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = name,
                                color = TextSageLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Search trigger button on top bar
                IconButton(
                    onClick = { searchActive = true },
                    modifier = Modifier.testTag("search_trigger_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = GoldAccent
                    )
                }
            }
        }
    }
}

// ==========================================
// HERO BANNER SECTION (البانر الرئيسي)
// ==========================================
@Composable
fun HeroBannerSection(
    isWide: Boolean = false,
    onListenQuranClicked: () -> Unit,
    onBrowseContentClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SurfaceOliveLight,
                        SurfaceOlive,
                        DarkBg
                    ),
                    radius = 1200f
                )
            )
            .border(1.dp, BorderGreen, RoundedCornerShape(24.dp))
            .padding(if (isWide) 32.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Texts and CTAs
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ActivePillBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "منصة ذاكرون الرقمية",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "وَاذْكُرِ اسْمَ رَبِّكَ",
                        color = GoldAccent,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 46.sp
                    )

                    Text(
                        text = "وَالذَّاكِرِينَ اللَّهَ كَثِيرًا وَالذَّاكِرَاتِ",
                        color = TextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "منصتك الإسلامية الشاملة — مقالات قيّمة، تلاوات خاشعة، مرئيات تعليمية، وأخبار تهم كل مسلم في حياته اليومية.",
                        color = TextSage,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.widthIn(max = 520.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.widthIn(max = 440.dp)
                    ) {
                        Button(
                            onClick = onListenQuranClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .testTag("btn_listen_quran")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                                Text(
                                    text = "الاستماع للقرآن",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onBrowseContentClicked,
                            border = BorderStroke(1.dp, BorderGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_browse_content")
                        ) {
                            Text(
                                text = "تصفح المحتوى",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Lantern swing animation on the right
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LanternIllumination(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hanging Decorative Lantern
                LanternIllumination(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 12.dp)
                )

                // Dynamic Calligraphy Frame Overlay
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
                    border = BorderStroke(1.dp, GoldAccentMuted.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "وَاذْكُرِ اسْمَ رَبِّكَ",
                            color = GoldAccent,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 38.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        )

                        Text(
                            text = "وَالذَّاكِرِينَ اللَّهَ كَثِيرًا وَالذَّاكِرَاتِ",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Description
                Text(
                    text = "منصتك الإسلامية الشاملة — مقالات، صوتيات، مرئيات، وأخبار. كل مسلم.",
                    color = TextSage,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .widthIn(max = 340.dp)
                )

                // Two core CTA buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onListenQuranClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_listen_quran")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Text(
                                text = "الاستماع للقرآن",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onBrowseContentClicked,
                        border = BorderStroke(1.dp, BorderGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_browse_content")
                    ) {
                        Text(
                            text = "تصفح المحتوى",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// CATEGORIES SECTION (تصفح حسب الفئة)
// ==========================================
@Composable
fun CategoriesSection(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        Pair("الكل", Icons.Default.Home),
        Pair("المقالات", Icons.Default.MenuBook),
        Pair("العقيدة", Icons.Default.Key),
        Pair("الفقه", Icons.Default.Gavel),
        Pair("صوتيات", Icons.Default.VolumeUp),
        Pair("مرئيات", Icons.Default.PlayCircle)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionTitleText(text = "تصفح حسب الفئة")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categories.forEach { (name, icon) ->
                val isSelected = selectedCategory == name
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) GoldAccent else SurfaceOliveLight
                )
                val strokeColor by animateColorAsState(
                    targetValue = if (isSelected) GoldAccent else BorderGreen
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.Black else TextSageLight
                )

                Surface(
                    color = bgColor,
                    border = BorderStroke(1.dp, strokeColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clickable { onCategorySelected(name) }
                        .testTag("category_$name")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = name,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// FEATURED ARTICLES SECTION (المقالات المميزة)
// ==========================================
@Composable
fun FeaturedArticlesSection(
    selectedCategory: String,
    bookmarkedIds: Set<String>,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: (Article) -> Unit
) {
    // Filter articles based on category choice
    val filteredArticles = StaticContent.articlesList.filter {
        selectedCategory == "الكل" || selectedCategory == "المقالات" || it.category == selectedCategory
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitleText(text = "مقالات مميزة")
            Text(
                text = "عرض الكل",
                color = GoldAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { /* No action */ }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredArticles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .border(1.dp, BorderGreen, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد مقالات في هذه الفئة حالياً",
                    color = TextSage,
                    fontSize = 13.sp
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                filteredArticles.forEach { article ->
                    val isFav = bookmarkedIds.contains(article.id)
                    ArticleCard(
                        article = article,
                        isBookmarked = isFav,
                        onClick = { onArticleClick(article) },
                        onBookmarkClick = { onBookmarkClick(article) }
                    )
                }
            }
        }
    }
}

@Composable
fun ArticleCard(
    article: Article,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Surface(
        color = SurfaceOlive,
        border = BorderStroke(1.dp, BorderGreen),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .width(280.dp)
            .clickable(onClick = onClick)
            .testTag("article_${article.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ActivePillBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = article.category,
                        color = EmeraldActive,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bookmark icon
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("fav_btn_${article.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (isBookmarked) GoldAccent else TextSage
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Article Title
            Text(
                text = article.title,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Article Excerpt
            Text(
                text = article.excerpt,
                color = TextSage,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(54.dp)
            )

            Divider(color = BorderGreen, modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.author,
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = TextSage,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = article.durationText,
                        color = TextSage,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// QURAN AUDIO PLAYER & PLAYLIST SECTION (الصوتيات والقرآن)
// ==========================================
@Composable
fun AudioPanelSection(
    isWide: Boolean = false,
    currentTrack: AudioTrack,
    isPlaying: Boolean,
    progress: Float,
    elapsedSeconds: Int,
    bookmarkedIds: Set<String>,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onTrackSelect: (AudioTrack) -> Unit,
    onBookmarkToggle: (AudioTrack) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionTitleText(text = "الصوتيات والقرآن")

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceOlive),
            border = BorderStroke(1.dp, BorderGreen),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Pane: The Player
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceOliveLight)
                            .padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "المشغّل الصوتي",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            DiskVisualizer(
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .size(110.dp)
                                    .padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = currentTrack.title,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = currentTrack.reciter,
                                color = TextSage,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Slider(
                                value = progress,
                                onValueChange = onSeek,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent,
                                    inactiveTrackColor = BorderGreen
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(elapsedSeconds),
                                    color = TextSage,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = currentTrack.durationText,
                                    color = TextSage,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                IconButton(
                                    onClick = onPrev,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("btn_prev")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "السابق",
                                        tint = TextWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onPlayPauseToggle,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(GoldAccent, CircleShape)
                                        .testTag("btn_play_pause")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                                        tint = Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onNext,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("btn_next")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "التالي",
                                        tint = TextWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Right Pane: Playlist
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "قائمة المقاطع",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        StaticContent.audioTracksList.forEach { track ->
                            val isSelected = track.id == currentTrack.id
                            val isFav = bookmarkedIds.contains(track.id)

                            TrackListItem(
                                track = track,
                                isSelected = isSelected,
                                isBookmarked = isFav,
                                onClick = { onTrackSelect(track) },
                                onBookmarkToggle = { onBookmarkToggle(track) }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // A. Left Column/Part: The Player
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceOliveLight)
                            .padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Title / Tag
                            Text(
                                text = "المشغّل الصوتي",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Animated Visual Disk Pattern
                            DiskVisualizer(
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .size(110.dp)
                                    .padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Selected sound title
                            Text(
                                text = currentTrack.title,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = currentTrack.reciter,
                                color = TextSage,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Audio Progress Bar / Slider
                            Slider(
                                value = progress,
                                onValueChange = onSeek,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent,
                                    inactiveTrackColor = BorderGreen
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                            )

                            // Time markers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(elapsedSeconds),
                                    color = TextSage,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = currentTrack.durationText,
                                    color = TextSage,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Audio player control buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                IconButton(
                                    onClick = onPrev,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("btn_prev")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "السابق",
                                        tint = TextWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onPlayPauseToggle,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(GoldAccent, CircleShape)
                                        .testTag("btn_play_pause")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                                        tint = Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onNext,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("btn_next")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "التالي",
                                        tint = TextWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // B. Right Column/Part: Audio Library list
                    Text(
                        text = "قائمة المقاطع",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    StaticContent.audioTracksList.forEach { track ->
                        val isSelected = track.id == currentTrack.id
                        val isFav = bookmarkedIds.contains(track.id)

                        TrackListItem(
                            track = track,
                            isSelected = isSelected,
                            isBookmarked = isFav,
                            onClick = { onTrackSelect(track) },
                            onBookmarkToggle = { onBookmarkToggle(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackListItem(
    track: AudioTrack,
    isSelected: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    val bgColor = if (isSelected) ActivePillBg else Color.Transparent
    val titleColor = if (isSelected) GoldAccent else TextWhite

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag("track_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Play indicator
                Icon(
                    imageVector = if (isSelected) Icons.Default.VolumeUp else Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = if (isSelected) EmeraldActive else TextSage,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        text = track.title,
                        color = titleColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.reciter,
                        color = TextSage,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = track.durationText,
                    color = TextSage,
                    fontSize = 11.sp
                )

                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("bookmark_track_${track.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "إضافة للمفضلة",
                        tint = if (isBookmarked) GoldAccent else TextSage,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// FEATURED VIDEOS SECTION (المرئيات المميزة)
// ==========================================
@Composable
fun FeaturedVideosSection(
    isWide: Boolean = false,
    bookmarkedIds: Set<String>,
    onVideoClick: (VideoItem) -> Unit,
    onBookmarkToggle: (VideoItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitleText(text = "المرئيات المميزة")
            Text(
                text = "عرض الكل",
                color = GoldAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { /* No action */ }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane: Large Video card
                Box(
                    modifier = Modifier.weight(1.2f)
                ) {
                    val mainVideo = StaticContent.videosList[0]
                    VideoLargeCard(
                        video = mainVideo,
                        isBookmarked = bookmarkedIds.contains(mainVideo.id),
                        onVideoClick = { onVideoClick(mainVideo) },
                        onBookmarkToggle = { onBookmarkToggle(mainVideo) }
                    )
                }

                // Right Pane: Grid/Column of Small videos as list rows
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StaticContent.videosList.drop(1).forEach { video ->
                        VideoSmallCardRow(
                            video = video,
                            isBookmarked = bookmarkedIds.contains(video.id),
                            onVideoClick = { onVideoClick(video) },
                            onBookmarkToggle = { onBookmarkToggle(video) }
                        )
                    }
                }
            }
        } else {
            // Main video (Large Card)
            val mainVideo = StaticContent.videosList[0]
            VideoLargeCard(
                video = mainVideo,
                isBookmarked = bookmarkedIds.contains(mainVideo.id),
                onVideoClick = { onVideoClick(mainVideo) },
                onBookmarkToggle = { onBookmarkToggle(mainVideo) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary small videos row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StaticContent.videosList.drop(1).forEach { video ->
                    VideoSmallCard(
                        video = video,
                        isBookmarked = bookmarkedIds.contains(video.id),
                        onVideoClick = { onVideoClick(video) },
                        onBookmarkToggle = { onBookmarkToggle(video) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoSmallCardRow(
    video: VideoItem,
    isBookmarked: Boolean,
    onVideoClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceOlive,
        border = BorderStroke(1.dp, BorderGreen),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onVideoClick)
            .testTag("video_${video.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 74.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceOliveLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    tint = GoldAccent.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )

                // Time Duration
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xDD000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.durationText,
                        color = TextWhite,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.views,
                        color = TextSage,
                        fontSize = 11.sp
                    )

                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("bookmark_video_${video.id}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isBookmarked) GoldAccent else TextSage,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoLargeCard(
    video: VideoItem,
    isBookmarked: Boolean,
    onVideoClick: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    Surface(
        color = SurfaceOlive,
        border = BorderStroke(1.dp, BorderGreen),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onVideoClick)
            .testTag("video_${video.id}")
    ) {
        Column {
            // Video Thumbnail Simulation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(SurfaceOliveLight, DarkBg)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Background Mosque Vector / Silhouette canvas
                WaveformArt(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp)
                )

                // Giant play button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0x99000000), CircleShape)
                        .border(1.dp, GoldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "شاهد الفيديو",
                        tint = GoldAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Time Duration tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xAA000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = video.durationText,
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info details below
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.title,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("bookmark_video_${video.id}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "إضافة للمفضلة",
                            tint = if (isBookmarked) GoldAccent else TextSage,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = video.description,
                    color = TextSage,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VideoSmallCard(
    video: VideoItem,
    isBookmarked: Boolean,
    onVideoClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceOlive,
        border = BorderStroke(1.dp, BorderGreen),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .clickable(onClick = onVideoClick)
            .testTag("video_${video.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(SurfaceOliveLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    tint = GoldAccent.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )

                // Duration
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xDD000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.durationText,
                        color = TextWhite,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = video.title,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.views,
                        color = TextSage,
                        fontSize = 10.sp
                    )

                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("bookmark_video_${video.id}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isBookmarked) GoldAccent else TextSage,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// INTERACTIVE BOOKMARKS SHELF (المستندات والمحفوظات)
// ==========================================
@Composable
fun BookmarksShelfSection(
    bookmarks: List<BookmarkEntity>,
    onItemClick: (BookmarkEntity) -> Unit,
    onRemoveBookmark: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionTitleText(text = "ممتلكاتك ومحفوظاتك")

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceOlive),
            border = BorderStroke(1.dp, GoldAccentMuted.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "العناصر المضافة للمفضلة للدراسة والعودة لاحقاً (${bookmarks.size})",
                    color = TextSage,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                bookmarks.forEach { bookmark ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(bookmark) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val icon = when (bookmark.type) {
                                "article" -> Icons.Default.Article
                                "video" -> Icons.Default.PlayCircle
                                else -> Icons.Default.VolumeUp
                            }
                            val tagText = when (bookmark.type) {
                                "article" -> "مقال"
                                "video" -> "مرئي"
                                else -> "صوت"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ActivePillBg)
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = tagText,
                                    color = EmeraldActive,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text(
                                    text = bookmark.title,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = bookmark.info,
                                    color = TextSage,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { onRemoveBookmark(bookmark.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "حذف من المفضلة",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// FOOTER (التذييل)
// ==========================================
@Composable
fun FooterSection(
    onLinkClick: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Surface(
        color = SurfaceOlive,
        border = BorderStroke(1.dp, BorderGreen),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogoPattern(modifier = Modifier.size(32.dp))
                Text(
                    text = "ذاكرون",
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Text(
                text = "منصتك الإسلامية الشاملة — مقالات، صوتيات، مرئيات، وأخبار في جيب كل مسلم.",
                color = TextSage,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .widthIn(max = 280.dp)
            )

            // Social media buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                SocialPainterIconBtn(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_facebook),
                    contentDescription = "فيسبوك",
                    testTag = "btn_social_facebook",
                    onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://www.facebook.com/share/1Bbpg7HPNS/")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
                SocialPainterIconBtn(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_youtube),
                    contentDescription = "يوتيوب",
                    testTag = "btn_social_youtube",
                    onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://m.youtube.com/@althakron9")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
                SocialIconBtn(icon = Icons.Default.Send, onClick = { /* Action */ })
                SocialIconBtn(icon = Icons.Default.Chat, onClick = { /* Action */ }) // Twitter fallback
                SocialIconBtn(icon = Icons.Default.Email, onClick = { /* Action */ })
                SocialIconBtn(icon = Icons.Default.Language, onClick = { /* Action */ })
            }

            Divider(color = BorderGreen, modifier = Modifier.padding(vertical = 16.dp))

            // Quick Links
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                val links = listOf("الرئيسية", "المقالات", "الصوتيات", "المرئيات")
                links.forEach { link ->
                    Text(
                        text = link,
                        color = TextSageLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onLinkClick(link) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Copyright Text
            Text(
                text = "جميع الحقوق محفوظة © 2026 ذاكرون.",
                color = TextSage,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SocialPainterIconBtn(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(SurfaceOliveLight, CircleShape)
            .border(1.dp, BorderGreen, CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = GoldAccent,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SocialIconBtn(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(SurfaceOliveLight, CircleShape)
            .border(1.dp, BorderGreen, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector ?: icon,
            contentDescription = null,
            tint = GoldAccent,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ==========================================
// IMMERSIVE READER DIALOG (مستعرض المقال)
// ==========================================
@Composable
fun ArticleReaderDialog(
    article: Article,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth > 600.dp

            Surface(
                color = DarkBg,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = if (isWide) 48.dp else 16.dp, vertical = 16.dp)
                        .widthIn(max = 760.dp)
                        .align(Alignment.Center)
                ) {
                    // Header of dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "عودة",
                                tint = TextWhite
                            )
                        }

                        Row {
                            IconButton(onClick = onToggleBookmark) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "المفضلة",
                                    tint = if (isBookmarked) GoldAccent else TextWhite
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable Article body
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = if (isWide) 24.dp else 10.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ActivePillBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = article.category,
                                    color = EmeraldActive,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = article.title,
                                color = GoldAccent,
                                fontSize = if (isWide) 28.sp else 24.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = if (isWide) 38.sp else 32.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "بقلم: ${article.author}",
                                    color = TextSageLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "•",
                                    color = BorderGreen
                                )
                                Text(
                                    text = "مدة القراءة: ${article.durationText}",
                                    color = TextSage,
                                    fontSize = 11.sp
                                )
                            }

                            Divider(color = BorderGreen, modifier = Modifier.padding(vertical = 16.dp))

                            // Actual full text
                            Text(
                                text = article.content,
                                color = TextWhite,
                                fontSize = if (isWide) 16.sp else 15.sp,
                                lineHeight = if (isWide) 28.sp else 26.sp,
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }

                    // Done Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "متابعة تصفح ذاكرون",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// IMMERSIVE SIMULATED VIDEO DIALOG
// ==========================================
@Composable
fun SimulatedVideoPlayerDialog(
    video: VideoItem,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    var playing by remember { mutableStateOf(true) }
    var secondsPlayed by remember { mutableStateOf(0) }
    var currentProgress by remember { mutableStateOf(0.1f) }

    LaunchedEffect(playing) {
        if (playing) {
            while (true) {
                delay(1000)
                secondsPlayed += 1
                currentProgress = (secondsPlayed.toFloat() / 200f).coerceIn(0f, 1f)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth > 600.dp

            Surface(
                color = DarkBg,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = if (isWide) 48.dp else 16.dp, vertical = 16.dp)
                        .widthIn(max = 760.dp)
                        .align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "عودة",
                                tint = TextWhite
                            )
                        }

                        IconButton(onClick = onToggleBookmark) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "المفضلة",
                                tint = if (isBookmarked) GoldAccent else TextWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cinematic simulated screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isWide) 340.dp else 210.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, BorderGreen, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Visual elements
                        WaveformArt(modifier = Modifier.fillMaxSize())

                        // Sound icon overlay
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .background(Color(0x77000000), CircleShape)
                                .border(1.dp, GoldAccent, CircleShape)
                                .clickable { playing = !playing },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "",
                                tint = GoldAccent,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Progress slider overlay at bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(secondsPlayed),
                                    color = TextWhite,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = video.durationText,
                                    color = TextWhite,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { currentProgress },
                                color = GoldAccent,
                                trackColor = BorderGreen,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = video.title,
                        color = GoldAccent,
                        fontSize = if (isWide) 24.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = if (isWide) 32.sp else 28.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldPillBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "بث مميز",
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                              )
                        }

                        Text(
                            text = "•",
                            color = BorderGreen
                        )

                        Text(
                            text = video.views,
                            color = TextSage,
                            fontSize = 11.sp
                        )
                    }

                    Divider(color = BorderGreen, modifier = Modifier.padding(vertical = 16.dp))

                    Text(
                        text = video.description,
                        color = TextWhite,
                        fontSize = if (isWide) 15.sp else 14.sp,
                        lineHeight = if (isWide) 24.sp else 22.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "إغلاق المشاهدة",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TECHNICAL VISUAL ORNAMENT DRAWINGS & HELPER PATTERNS
// ==========================================

@Composable
fun LogoPattern(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizeMin = size.minDimension
        val strokeW = sizeMin * 0.08f
        val center = Offset(size.width / 2, size.height / 2)

        // Draw rotated square-stars to form beautiful Islamic star
        drawRect(
            color = GoldAccent,
            topLeft = Offset(center.x - sizeMin * 0.35f, center.y - sizeMin * 0.35f),
            size = Size(sizeMin * 0.7f, sizeMin * 0.7f),
            style = Stroke(width = strokeW)
        )

        rotate(45f, center) {
            drawRect(
                color = GoldAccent,
                topLeft = Offset(center.x - sizeMin * 0.35f, center.y - sizeMin * 0.35f),
                size = Size(sizeMin * 0.7f, sizeMin * 0.7f),
                style = Stroke(width = strokeW)
            )
        }

        // Concentric inner circle
        drawCircle(
            color = EmeraldActive,
            radius = sizeMin * 0.18f,
            style = Stroke(width = strokeW * 0.7f)
        )
    }
}

@Composable
fun LanternIllumination(modifier: Modifier = Modifier) {
    // Beautiful infinite subtle swing animation
    val infiniteTransition = rememberInfiniteTransition(label = "swing")
    val angle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swing_angle"
    )

    Canvas(
        modifier = modifier.graphicsLayer(rotationZ = angle)
    ) {
        val w = size.width
        val h = size.height

        // Hanging thread
        drawLine(
            color = GoldAccentMuted,
            start = Offset(w / 2, 0f),
            end = Offset(w / 2, h * 0.25f),
            strokeWidth = 2.dp.toPx()
        )

        // Lantern Dome / Top Cap
        val pathTop = Path().apply {
            moveTo(w * 0.35f, h * 0.25f)
            quadraticTo(w / 2, h * 0.12f, w * 0.65f, h * 0.25f)
            lineTo(w * 0.35f, h * 0.25f)
        }
        drawPath(pathTop, color = GoldAccent)

        // Main glass body
        val pathBody = Path().apply {
            moveTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.25f, h * 0.55f)
            lineTo(w * 0.35f, h * 0.75f)
            lineTo(w * 0.65f, h * 0.75f)
            lineTo(w * 0.75f, h * 0.55f)
            lineTo(w * 0.65f, h * 0.25f)
            close()
        }
        drawPath(
            pathBody,
            brush = Brush.radialGradient(
                colors = listOf(
                    GoldAccent.copy(alpha = 0.5f),
                    SurfaceOliveLight.copy(alpha = 0.1f)
                ),
                center = Offset(w / 2, h * 0.5f),
                radius = w * 0.4f
            )
        )
        drawPath(pathBody, color = GoldAccent, style = Stroke(width = 2.dp.toPx()))

        // Glow center representing candle
        drawCircle(
            color = GoldAccent,
            radius = w * 0.10f,
            center = Offset(w / 2, h * 0.5f)
        )

        // Bottom element
        val pathBottom = Path().apply {
            moveTo(w * 0.35f, h * 0.75f)
            quadraticTo(w / 2, h * 0.85f, w * 0.65f, h * 0.75f)
        }
        drawPath(pathBottom, color = GoldAccent, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun DiskVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 6000 else 100000000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Canvas(
        modifier = modifier.graphicsLayer(rotationZ = angle)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Outer record disk ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SurfaceOlive,
                    DarkBg,
                    SurfaceOliveLight
                )
            ),
            radius = radius
        )
        drawCircle(
            color = BorderGreen,
            radius = radius,
            style = Stroke(width = 2.dp.toPx())
        )

        // Inner grooves
        for (i in 1..4) {
            drawCircle(
                color = BorderGreen.copy(alpha = 0.3f),
                radius = radius * (1f - i * 0.18f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Gold center core ornament
        drawCircle(
            color = GoldAccent,
            radius = radius * 0.22f
        )
        drawCircle(
            color = Color.Black,
            radius = radius * 0.08f
        )
    }
}

@Composable
fun WaveformArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(0f, h * 0.7f)
            quadraticTo(w * 0.2f, h * 0.3f, w * 0.4f, h * 0.61f)
            quadraticTo(w * 0.6f, h * 0.85f, w * 0.8f, h * 0.42f)
            quadraticTo(w * 0.9f, h * 0.22f, w, h * 0.5f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    EmeraldActive.copy(alpha = 0.23f),
                    Color.Transparent
                )
            )
        )

        drawPath(
            path = path,
            color = GoldAccent.copy(alpha = 0.15f),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun SectionTitleText(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(vertical = 12.dp)
    ) {
        // Simple elegant gold marker
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .background(GoldAccent, RoundedCornerShape(2.dp))
        )

        Text(
            text = text,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

// Helper to format remaining duration nicely
fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

@Composable
fun AmbientSpiritualParticles(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "particles")
    val animTime = 16000 // 16 seconds full loop
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animTime, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Remember stable particle parameters that don't change across recompositions
    val particles = remember {
        List(25) {
            ParticleData(
                relX = (0.01f + Math.random() * 0.98f).toFloat(),
                relY = (0.01f + Math.random() * 0.98f).toFloat(),
                speed = (0.05f + Math.random() * 0.15f).toFloat(),
                maxAlpha = (0.10f + Math.random() * 0.40f).toFloat(),
                radius = (3f + Math.random() * 7f).toFloat(),
                pulseSpeed = (1f + Math.random() * 2.5f).toFloat()
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            // Simulating float up animation
            var yOffset = p.relY - (progress * p.speed)
            if (yOffset < 0f) {
                yOffset += 1.0f // wrapped around bottom
            }
            val x = p.relX * w
            val y = yOffset * h

            // Periodic alpha breath animation
            val pulse = kotlin.math.sin((progress * 2 * Math.PI * p.pulseSpeed).toFloat())
            val alpha = (p.maxAlpha * (0.4f + 0.6f * pulse)).coerceIn(0f, 1f)

            // Draw particle point
            drawCircle(
                color = GoldAccent,
                radius = p.radius,
                center = Offset(x, y),
                alpha = alpha
            )

            // Draw custom aura to convey gentle lighting
            if (p.radius > 5f) {
                drawCircle(
                    color = GoldAccentMuted,
                    radius = p.radius * 2.2f,
                    center = Offset(x, y),
                    alpha = alpha * 0.25f
                )
            }
        }
    }
}

data class ParticleData(
    val relX: Float,
    val relY: Float,
    val speed: Float,
    val maxAlpha: Float,
    val radius: Float,
    val pulseSpeed: Float
)

