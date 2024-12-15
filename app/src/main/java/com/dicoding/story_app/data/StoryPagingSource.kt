package com.dicoding.story_app.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dicoding.story_app.data.pref.UserPreference
import com.dicoding.story_app.data.response.ListStoryItem
import com.dicoding.story_app.remote.ApiService
import kotlinx.coroutines.flow.first

@Suppress("unused")
class StoryPagingSource(private val apiService: ApiService, private val userPreference: UserPreference) :
    PagingSource<Int, ListStoryItem>() {

    private companion object {
        const val INITIAL_PAGE_INDEX = 1
        const val LOCATION = 0
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListStoryItem> {
        return try {
            val position = params.key ?: INITIAL_PAGE_INDEX
            val locationMode = params.key ?: LOCATION
            val token = userPreference.getSession().first().token
            val response = apiService.getStories(token, position, params.loadSize, locationMode)

            if (response.isSuccessful) {
                val responseData = response.body()!!.listStory
                LoadResult.Page(
                    data = responseData,
                    prevKey = if (position == INITIAL_PAGE_INDEX) null else position - 1,
                    nextKey = if (responseData.isEmpty()) null else position + 1
                )
            } else {
                LoadResult.Error(Exception("Error: ${response.message()}"))
            }
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ListStoryItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}