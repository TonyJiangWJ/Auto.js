package org.autojs.autojs.ui.project;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.autojs.autojs.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * option选项适配器，用于在RecyclerView中添加单选选项
 * Powered by ChatGPT3.5
 */
public class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.OptionViewHolder> {
    private static final String TAG = "OptionAdapter";

    private final List<Option> options;

    public OptionAdapter(List<Option> options) {
        this.options = options;
        Log.d(TAG, "OptionAdapter: options size: " + this.options.size());
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: ");
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_option, parent, false);
        return new OptionViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder position: " + position);
        Option option = options.get(position);
        holder.bind(option);
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: current: " + options.size());
        return options.size();
    }

    static class OptionViewHolder extends RecyclerView.ViewHolder {

        private static final String TAG = "OptionViewHolder";
        private final CheckBox checkBox;
        private final TextView optionText;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox);
            optionText = itemView.findViewById(R.id.option_text);
        }

        void bind(final Option option) {
            Log.d(TAG, "bind option: " + option.getText());
            checkBox.setChecked(option.isSelected());
            optionText.setText(option.getText().split("permission.")[1]);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    option.setSelected(!option.isSelected());
                    checkBox.setChecked(option.isSelected());
                }
            });
        }
    }

}
