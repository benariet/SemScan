package org.example.semscan.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.example.semscan.R;
import org.example.semscan.data.model.Course;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseAdapter extends BaseAdapter {
    
    private Context context;
    private List<Course> courses;
    private LayoutInflater inflater;
    private SimpleDateFormat dateFormat;
    
    public CourseAdapter(Context context, List<Course> courses) {
        this.context = context;
        this.courses = courses;
        this.inflater = LayoutInflater.from(context);
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }
    
    @Override
    public int getCount() {
        return courses.size();
    }
    
    @Override
    public Object getItem(int position) {
        return courses.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_course, parent, false);
            holder = new ViewHolder();
            holder.textCourseName = convertView.findViewById(R.id.text_course_name);
            holder.textCourseCode = convertView.findViewById(R.id.text_course_code);
            holder.textCourseDescription = convertView.findViewById(R.id.text_course_description);
            holder.textCreatedDate = convertView.findViewById(R.id.text_created_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        Course course = courses.get(position);
        
        holder.textCourseName.setText(course.getCourseName());
        holder.textCourseCode.setText(course.getCourseCode());
        
        if (course.getDescription() != null && !course.getDescription().isEmpty()) {
            holder.textCourseDescription.setText(course.getDescription());
            holder.textCourseDescription.setVisibility(View.VISIBLE);
        } else {
            holder.textCourseDescription.setVisibility(View.GONE);
        }
        
        if (course.getCreatedAt() > 0) {
            String formattedDate = dateFormat.format(new Date(course.getCreatedAt()));
            holder.textCreatedDate.setText("Created: " + formattedDate);
            holder.textCreatedDate.setVisibility(View.VISIBLE);
        } else {
            holder.textCreatedDate.setVisibility(View.GONE);
        }
        
        return convertView;
    }
    
    static class ViewHolder {
        TextView textCourseName;
        TextView textCourseCode;
        TextView textCourseDescription;
        TextView textCreatedDate;
    }
}

