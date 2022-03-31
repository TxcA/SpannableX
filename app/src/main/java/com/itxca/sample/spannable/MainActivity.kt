package com.itxca.sample.spannable

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.itxca.sample.spannable.databinding.MainActivityBinding


class MainActivity : AppCompatActivity() {

    private val fragments = listOf(
        "Code Sample" to CodeFragment.newInstance(),
        "Kotlin" to KotlinFragment.newInstance(),
        "Java" to JavaFragment.newInstance(),
    )

    private val viewBinding by lazy { MainActivityBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        repeat(fragments.size) { viewBinding.tab.addTab(viewBinding.tab.newTab()) }
        viewBinding.container.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position].second
        }

        TabLayoutMediator(viewBinding.tab, viewBinding.container) { layoutTab, position ->
            layoutTab.text = fragments[position].first
        }.attach()
    }

}