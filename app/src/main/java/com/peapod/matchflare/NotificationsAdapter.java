package com.peapod.matchflare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by piyushpoddar on 12/1/14.
 */
public class NotificationsAdapter extends BaseExpandableListAdapter {

    private Activity context;
    public Map<String, List> notificationCollections = new HashMap<String, List>();
    public List<String> headers;



    public NotificationsAdapter(Activity context) {
        this.context = context;
    }


    public Object getChild(int groupPosition, int childPosition) {
        return notificationCollections.get(headers.get(groupPosition)).get(childPosition);
    }


    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }


    public View getChildView(final int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        Object child = getChild(groupPosition, childPosition);

        LayoutInflater inflater = context.getLayoutInflater();

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.child_item, null);
        }


        TextView childTextView = (TextView) convertView.findViewById(R.id.notification_text);
        Style.toOpenSans(context,childTextView,"light");
        childTextView.setText(child.toString());

        if (child instanceof Match) {
            Match thisMatch = (Match) child;
            if (thisMatch.has_unseen != null && thisMatch.has_unseen.booleanValue()) {
                childTextView.setTextColor(context.getResources().getColor(R.color.matchflare_pink));
                ImageView notificationIcon = (ImageView) convertView.findViewById(R.id.notification_image);
                notificationIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.new_chat_icon));
            }
            else {
                childTextView.setTextColor(context.getResources().getColor(R.color.white));
                ImageView notificationIcon = (ImageView) convertView.findViewById(R.id.notification_image);
                notificationIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_launcher));
            }
        }

        return convertView;
    }

    public int getChildrenCount(int groupPosition) {
        return notificationCollections.get(headers.get(groupPosition)).size();
    }

    public Object getGroup(int groupPosition) {
        return headers.get(groupPosition);
    }

    public int getGroupCount() {
        return headers.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String header = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.group_item,
                    null);
        }
        TextView item = (TextView) convertView.findViewById(R.id.notification_header);
        Style.toOpenSans(context,item,"regular");
        item.setText(header);
        return convertView;
    }

    public boolean hasStableIds() {
        return true;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
