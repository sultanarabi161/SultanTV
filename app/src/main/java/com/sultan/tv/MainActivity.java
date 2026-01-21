<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#000000"
    android:padding="10dp">

    <RelativeLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="10dp">

        <ImageView
            android:id="@+id/imgLogo"
            android:layout_width="30dp"
            android:layout_height="30dp"
            android:src="@drawable/ic_launcher_foreground" 
            android:layout_alignParentStart="true"
            android:layout_centerVertical="true"/>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Sultan TV"
            android:textColor="#FFFFFF"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_centerInParent="true"/>

        <ImageView
            android:id="@+id/btnInfo"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:src="@android:drawable/ic_menu_info_details"
            android:layout_alignParentEnd="true"
            android:layout_centerVertical="true"
            app:tint="#FFFFFF" xmlns:app="http://schemas.android.com/apk/res-auto"/>
    </RelativeLayout>

    <EditText
        android:id="@+id/etSearch"
        android:layout_width="match_parent"
        android:layout_height="45dp"
        android:background="@drawable/bg_search"
        android:hint="Search Channels..."
        android:textColorHint="#888888"
        android:textColor="#FFFFFF"
        android:paddingStart="15dp"
        android:textSize="14sp"
        android:maxLines="1"
        android:inputType="text"
        android:layout_marginBottom="10dp"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvCategories"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginBottom="10dp"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvChannels"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
