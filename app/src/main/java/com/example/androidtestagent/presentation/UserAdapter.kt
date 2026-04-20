package com.example.androidtestagent.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.androidtestagent.data.model.User
import com.example.androidtestagent.databinding.ItemUserBinding

class UserAdapter : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            val initial = user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            binding.tvAvatar.text  = initial
            binding.tvName.text    = user.name
            binding.tvEmail.text   = user.canonicalEmail
            binding.chipRole.text  = user.role.name

            // Tint chip for admins
            val chipColor = if (user.role == User.Role.ADMIN) {
                com.google.android.material.R.attr.colorTertiaryContainer
            } else {
                com.google.android.material.R.attr.colorSecondaryContainer
            }
            val context = binding.root.context
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(chipColor, typedValue, true)
            binding.chipRole.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(typedValue.data)
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
