package jp.elestyle.androidapp.elepaydemoapp.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import jp.elestyle.androidapp.elepaydemoapp.R
import jp.elestyle.androidapp.elepaydemoapp.data.Product

class ProductListItemHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {
    val imageView: ImageView = containerView.findViewById(R.id.product_list_item_image)
    val titleView: TextView = containerView.findViewById(R.id.product_list_item_title)
    val priceView: TextView = containerView.findViewById(R.id.product_list_item_price)
    val priceButton: Button = containerView.findViewById(R.id.product_list_item_button_buy)
}

interface ProductListAdapterListener {
    fun onProductListItemPriceButtonClick(itemIndex: Int)
}

class ProductListAdapter(private val listener: ProductListAdapterListener?) : RecyclerView.Adapter<ProductListItemHolder>() {
    private val productList = listOf(
            Product("", "MOORING SMART MATTRESS PAD S", "1"),
            Product("", "MOORING SMART MATTRESS PAD D", "100")
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductListItemHolder {
        return ProductListItemHolder(LayoutInflater
                .from(parent.context)
                .inflate(R.layout.product_list_item_view, parent, false))
    }

    override fun getItemCount(): Int = productList.count()

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProductListItemHolder, position: Int) {
        productList[position].also { product ->
//            holder.imageView.setImageBitmap(null)
//            ImageLoader.loadImage(product.imageUrl, holder.imageView.context) { holder.imageView.setImageBitmap(it) }
            holder.imageView.setImageResource(R.mipmap.product_sample_img)

            holder.titleView.text = product.title
            holder.priceView.text = "¥${product.price}"

            if (!holder.priceButton.hasOnClickListeners()) {
                holder.priceButton.setOnClickListener {
                    listener?.onProductListItemPriceButtonClick(position)
                }
            }
        }
    }

    fun getItemAt(index: Int): Product = productList[index]

}