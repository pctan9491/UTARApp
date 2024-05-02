package com.example.utarapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
    private List<Comment> commentsList;

    public CommentsAdapter(List<Comment> commentsList) {
        this.commentsList = commentsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = commentsList.get(position);
        holder.commentText.setText(comment.getCommentText());
        holder.userName.setText(comment.getUserName());
        if (comment.getTimestamp() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault());
            String dateString = dateFormat.format(comment.getTimestamp());
            holder.commentDateStamp.setText(dateString);
        } else {
            holder.commentDateStamp.setText(""); // Handle null case or set to a default value
        }
        // Optionally, handle userId to display user name or other info
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView commentText;
        public TextView userName;
        public TextView commentDateStamp;

        public ViewHolder(View itemView) {
            super(itemView);
            commentText = itemView.findViewById(R.id.commentText);
            userName = itemView.findViewById(R.id.commentUserName);
            commentDateStamp = itemView.findViewById(R.id.commentDateStamp);
        }
    }
}

