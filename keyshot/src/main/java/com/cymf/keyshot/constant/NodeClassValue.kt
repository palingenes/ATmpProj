package com.cymf.keyshot.constant

object NodeClassValue {

    val ViewGroup = "android.view.ViewGroup"

    // 基础布局
    val LinearLayout = "android.widget.LinearLayout"
    val RelativeLayout = "android.widget.RelativeLayout"
    val FrameLayout = "android.widget.FrameLayout"
    val TableLayout = "android.widget.TableLayout"
    val AbsoluteLayout = "android.widget.AbsoluteLayout"
    val GridLayout = "android.widget.GridLayout"

    // 滚动相关
    val ScrollView = "android.widget.ScrollView"
    val HorizontalScrollView = "android.widget.HorizontalScrollView"
    val NestedScrollView = "androidx.core.widget.NestedScrollView"
    val NestedScrollView2 = "android.support.v4.widget.NestedScrollView"

    val RecyclerView = "androidx.recyclerview.widget.RecyclerView"
    val RecyclerView2 = "android.support.v7.widget.RecyclerView"

    val ViewPager2 = "androidx.viewpager2.widget.ViewPager2"
    val ViewPager = "androidx.viewpager.widget.ViewPager"

    val ConstraintLayout = "androidx.constraintlayout.widget.ConstraintLayout"
    val ConstraintLayout2 = "android.support.constraint.ConstraintLayout"

    // Material Design 容器
    val CoordinatorLayout = "com.google.android.material.appbar.CoordinatorLayout"
    val AppBarLayout = "com.google.android.material.appbar.AppBarLayout"
    val BottomSheetLayout = "com.google.android.material.bottomsheet.BottomSheetLayout"
    val NavigationView = "com.google.android.material.navigation.NavigationView"

    // Fragment 容器
    val FragmentContainerView = "androidx.fragment.app.FragmentContainerView"
    val FragmentTabHost = "androidx.fragment.app.FragmentTabHost"
    val FragmentPagerAdapter =
        "androidx.fragment.app.FragmentPagerAdapter\$FragmentWrapperPagerAdapter" // 特殊情况


    //-------------------------------------------------------------

    val View = "android.view.View"
    val TextView = "android.widget.TextView"
    val ImageView = "android.widget.ImageView"
    val Button = "android.widget.Button"
    val EditText = "android.widget.EditText"
    val CheckBox = "android.widget.CheckBox"
    val RadioButton = "android.widget.RadioButton"
    val Switch = "android.widget.Switch"
    val ProgressBar = "android.widget.ProgressBar"
    val SeekBar = "android.widget.SeekBar"
    val CompoundButton = "android.widget.CompoundButton"
    val ImageButton = "android.widget.ImageButton"
    val CheckedTextView = "android.widget.CheckedTextView"
    val RatingBar = "android.widget.RatingBar"
    val AutoCompleteTextView = "android.widget.AutoCompleteTextView"
    val MultiAutoCompleteTextView = "android.widget.MultiAutoCompleteTextView"
    val DatePicker = "android.widget.DatePicker"
    val TimePicker = "android.widget.TimePicker"
    val NumberPicker = "android.widget.NumberPicker"
    val Toolbar = "android.widget.Toolbar"
    val Spinner = "android.widget.Spinner"
    val TabWidget = "android.widget.TabWidget"
    val DialerFilter = "android.widget.DialerFilter"
    val SearchView = "android.widget.SearchView"
    val VideoView = "android.widget.VideoView"
    val WebViewLikes = listOf("android.widget.WebView", "android.webkit.WebView")
    val WebView = "android.widget.WebView"
    val WebView2 = "android.webkit.WebView"
    val Gallery = "android.widget.Gallery"
    val GridView = "android.widget.GridView"
    val ListView = "android.widget.ListView"
    val ExpandableListView = "android.widget.ExpandableListView"
    val AbsListView = "android.widget.AbsListView"


    //-------------------------------------------------------------


    // 所有已知的 ViewGroup 类型
    val viewGroups: Map<String, String> = mapOf(
        "ViewGroup" to ViewGroup,
        "LinearLayout" to LinearLayout,
        "RelativeLayout" to RelativeLayout,
        "FrameLayout" to FrameLayout,
        "ScrollView" to ScrollView,
        "HorizontalScrollView" to HorizontalScrollView,
        "NestedScrollView" to NestedScrollView,
        "NestedScrollView2" to NestedScrollView2,
        "RecyclerView" to RecyclerView,
        "RecyclerView2" to RecyclerView2,
        "ViewPager2" to ViewPager2,
        "ViewPager" to ViewPager,
        "ConstraintLayout" to ConstraintLayout,
        "ConstraintLayout2" to ConstraintLayout2,
        "CoordinatorLayout" to CoordinatorLayout,
        "AppBarLayout" to AppBarLayout,
        "BottomSheetLayout" to BottomSheetLayout,
        "NavigationView" to NavigationView,
        "FragmentContainerView" to FragmentContainerView,
        "FragmentTabHost" to FragmentTabHost,
        "FragmentPagerAdapter" to FragmentPagerAdapter
    )

    // 所有已知的普通 View 类型
    val views: Map<String, String> = mapOf(
        "View" to View,
        "TextView" to TextView,
        "ImageView" to ImageView,
        "Button" to Button,
        "EditText" to EditText,
        "CheckBox" to CheckBox,
        "RadioButton" to RadioButton,
        "Switch" to Switch,
        "ProgressBar" to ProgressBar,
        "SeekBar" to SeekBar,
        "CompoundButton" to CompoundButton,
        "ImageButton" to ImageButton,
        "CheckedTextView" to CheckedTextView,
        "RatingBar" to RatingBar,
        "AutoCompleteTextView" to AutoCompleteTextView,
        "MultiAutoCompleteTextView" to MultiAutoCompleteTextView,
        "DatePicker" to DatePicker,
        "TimePicker" to TimePicker,
        "NumberPicker" to NumberPicker,
        "Toolbar" to Toolbar,
        "Spinner" to Spinner,
        "TabWidget" to TabWidget,
        "DialerFilter" to DialerFilter,
        "SearchView" to SearchView,
        "VideoView" to VideoView,
        "WebView" to WebView,
        "WebView2" to WebView2,
        "Gallery" to Gallery,
        "GridView" to GridView,
        "ListView" to ListView,
        "ExpandableListView" to ExpandableListView,
        "AbsListView" to AbsListView
    )
}