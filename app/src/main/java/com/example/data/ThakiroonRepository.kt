package com.example.data

import kotlinx.coroutines.flow.Flow

class ThakiroonRepository(private val bookmarkDao: BookmarkDao) {
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun insertBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(id: String) {
        bookmarkDao.deleteBookmarkById(id)
    }
}
