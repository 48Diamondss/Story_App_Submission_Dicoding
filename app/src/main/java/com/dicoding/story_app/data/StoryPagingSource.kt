package com.dicoding.story_app.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dicoding.story_app.data.response.ListStoryItem

class StoryPagingSource(
    private val apiService: StoryRepository,
    private val token: String
) : PagingSource<Int, ListStoryItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListStoryItem> {
        return try {
            val page = params.key ?: 1
            val response = apiService.getStories(token) // Fetch stories
            val data = response.listStory

            LoadResult.Page(
                data = data,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (data.isEmpty()) null else page + 1
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ListStoryItem>): Int? {

        return state.anchorPosition?.let { position ->
            val page = state.closestPageToPosition(position)

            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
    }

}