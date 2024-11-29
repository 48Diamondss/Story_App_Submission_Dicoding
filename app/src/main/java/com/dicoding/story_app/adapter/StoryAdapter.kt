package com.dicoding.story_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dicoding.story_app.data.response.ListStoryItem
import com.dicoding.story_app.databinding.IcardStoryBinding

class StoryAdapter(
    private var stories: List<ListStoryItem>,
    private val onItemClicked: (ListStoryItem) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    inner class StoryViewHolder(private val binding: IcardStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(story: ListStoryItem) {
            with(binding) {
                tvItemName.text = story.name
                tvItemDescription.text = story.description
                Glide.with(itemView.context)
                    .load(story.photoUrl)
                    .centerCrop()
                    .into(ivItemPhoto)

                root.setOnClickListener {
                    onItemClicked(story)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = IcardStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(stories[position])
    }

    override fun getItemCount(): Int = stories.size

    // Method to update the list
    fun updateStories(newStories: List<ListStoryItem>) {
        val diffResult = DiffUtil.calculateDiff(StoryDiffCallback(stories, newStories))
        stories = newStories
        diffResult.dispatchUpdatesTo(this)
    }


    class StoryDiffCallback(
        private val oldList: List<ListStoryItem>,
        private val newList: List<ListStoryItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

}


