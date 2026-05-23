package com.example.viewmodel

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThakiroonViewModel(private val repository: ThakiroonRepository) : ViewModel() {

    // Filtering
    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Bookmarks loaded from Room DB
    val bookmarksList: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookmarkedIds: StateFlow<Set<String>> = bookmarksList
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    // Immersive Reading Modal State
    private val _selectedArticle = MutableStateFlow<Article?>(null)
    val selectedArticle: StateFlow<Article?> = _selectedArticle.asStateFlow()

    // Immersive Video Modal State
    private val _selectedVideo = MutableStateFlow<VideoItem?>(null)
    val selectedVideo: StateFlow<VideoItem?> = _selectedVideo.asStateFlow()

    // Audio Player State
    private val _currentTrack = MutableStateFlow(StaticContent.audioTracksList.first())
    val currentTrack: StateFlow<AudioTrack> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var playbackJob: Job? = null

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openArticle(article: Article?) {
        _selectedArticle.value = article
    }

    fun openVideo(video: VideoItem?) {
        _selectedVideo.value = video
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false

    private fun releaseMediaPlayer() {
        playbackJob?.cancel()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                     it.stop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            it.release()
        }
        mediaPlayer = null
        isPrepared = false
    }

    private fun playUrl(url: String) {
        releaseMediaPlayer()
        if (url.isEmpty()) return

        try {
            val mp = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener {
                    isPrepared = true
                    startPlayback()
                }
                setOnCompletionListener {
                    nextTrack()
                }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    releaseMediaPlayer()
                    true
                }
            }
            mediaPlayer = mp
            mp.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    private fun startPlayback() {
        val mp = mediaPlayer
        if (mp != null && isPrepared) {
            try {
                mp.start()
                _isPlaying.value = true
                startPlaybackTracking()
            } catch (e: Exception) {
                e.printStackTrace()
                _isPlaying.value = false
            }
        }
    }

    private fun startPlaybackTracking() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (_isPlaying.value) {
                val mp = mediaPlayer
                if (mp != null && isPrepared) {
                    try {
                        val currentPos = mp.currentPosition
                        val duration = mp.duration
                        if (duration > 0) {
                            _elapsedSeconds.value = currentPos / 1000
                            _playbackProgress.value = currentPos.toFloat() / duration.toFloat()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(500)
            }
        }
    }

    // Audio Player controls
    fun selectTrack(track: AudioTrack) {
        _currentTrack.value = track
        _playbackProgress.value = 0f
        _elapsedSeconds.value = 0
        _isPlaying.value = true
        playUrl(track.url)
    }

    fun togglePlayPause() {
        setPlaying(!_isPlaying.value)
    }

    fun setPlaying(play: Boolean) {
        _isPlaying.value = play
        if (play) {
            val mp = mediaPlayer
            if (mp != null && isPrepared) {
                try {
                    mp.start()
                    startPlaybackTracking()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                playUrl(_currentTrack.value.url)
            }
        } else {
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            playbackJob?.cancel()
        }
    }

    fun nextTrack() {
        val currentInd = StaticContent.audioTracksList.indexOfFirst { it.id == _currentTrack.value.id }
        if (currentInd != -1 && currentInd < StaticContent.audioTracksList.lastIndex) {
            selectTrack(StaticContent.audioTracksList[currentInd + 1])
        } else {
            selectTrack(StaticContent.audioTracksList.first()) // Loop back
        }
    }

    fun previousTrack() {
        val currentInd = StaticContent.audioTracksList.indexOfFirst { it.id == _currentTrack.value.id }
        if (currentInd != -1 && currentInd > 0) {
            selectTrack(StaticContent.audioTracksList[currentInd - 1])
        } else {
            selectTrack(StaticContent.audioTracksList.last()) // Go to last
        }
    }

    fun seekTo(progress: Float) {
        val totalSecs = _currentTrack.value.durationSeconds
        _playbackProgress.value = progress.coerceIn(0f, 1f)
        val mp = mediaPlayer
        if (mp != null && isPrepared) {
            val seekPosition = (progress * mp.duration).toInt()
            mp.seekTo(seekPosition)
            _elapsedSeconds.value = seekPosition / 1000
        } else {
            _elapsedSeconds.value = (progress * totalSecs).toInt()
        }
    }

    // Bookmarking Action
    fun toggleBookmark(id: String, type: String, title: String, info: String) {
        viewModelScope.launch {
            if (bookmarkedIds.value.contains(id)) {
                repository.deleteBookmark(id)
            } else {
                repository.insertBookmark(BookmarkEntity(id, type, title, info))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseMediaPlayer()
    }
}

class ThakiroonViewModelFactory(private val repository: ThakiroonRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThakiroonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThakiroonViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
