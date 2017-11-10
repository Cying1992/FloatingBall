package com.cying.floatingball

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import com.cying.lightorm.LightORM
import kotlinx.android.synthetic.main.activity_ad_test.*
import org.jetbrains.anko.toast

/**
 * Created by Cying on 17/11/10.
 */
class ADTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ad_test)
        val adapter = ArrayAdapter<AdBlockInfo>(this, android.R.layout.simple_list_item_1)

        clear_ad_db.setOnClickListener { _ ->
            LightORM.getInstance().deleteAll(AdBlockInfo::class.java)
            toast("清除广告Window数据库成功")
            adapter.clear()
            App.adBlockInfos.clear()
        }
        ad_test.setOnClickListener { _ ->
            toast("点击了测试关闭广告")
        }

        list_view.adapter = adapter
        adapter.addAll(App.adBlockInfos)
        list_view.setOnItemLongClickListener { parent, view, position, id ->
            val item = adapter.getItem(position)
            LightORM.getInstance().delete(item)
            toast("已为你删除$item")
            adapter.remove(item)
            App.adBlockInfos.remove(item)
            true
        }
    }
}