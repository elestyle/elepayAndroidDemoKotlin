package jp.elestyle.androidapp.elepaydemoapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.elestyle.androidapp.elepaydemoapp.R
import jp.elestyle.androidapp.elepaydemoapp.ui.adapter.ProductListAdapter
import jp.elestyle.androidapp.elepaydemoapp.ui.adapter.ProductListAdapterListener

class ProductListActivity : AppCompatActivity() {

    private lateinit var adapter: ProductListAdapter
    private val adapterListener: ProductListAdapterListener = object : ProductListAdapterListener {
        override fun onProductListItemPriceButtonClick(itemIndex: Int) {
            val product = adapter.getItemAt(itemIndex)
            Log.d("ProductListActivity", "onItemPriceButtonClick: ${product.title}, ${product.price}")
            Intent(this@ProductListActivity, PaymentActivity::class.java)
                    .apply {
                        putExtra(PaymentActivity.INTENT_KEY_AMOUNT, product.price)
                    }
                    .also { startActivity(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_product_list)

        val productListView = findViewById<RecyclerView>(R.id.activity_product_list_view)
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        productListView.layoutManager = layoutManager
        productListView.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
        adapter = ProductListAdapter(adapterListener)
        productListView.adapter = adapter

    }
}