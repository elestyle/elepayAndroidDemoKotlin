package jp.elestyle.androidapp.elepaydemoapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import jp.elestyle.androidapp.elepaydemoapp.R
import jp.elestyle.androidapp.elepaydemoapp.data.PaymentMethodItemData
import jp.elestyle.androidapp.elepaydemoapp.data.SupportedPaymentMethod

class PaymentMethodItemHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {
    val imageButton: ImageButton = containerView.findViewById(R.id.paymentMethodListItemImageButton)
}

interface PaymentMethodListAdapterListener {
    fun onSelectPaymentMethod(method: SupportedPaymentMethod)
}

class PaymentMethodListAdapter(private val listener: PaymentMethodListAdapterListener) : RecyclerView.Adapter<PaymentMethodItemHolder>() {
    private val list = listOf(
            PaymentMethodItemData(R.mipmap.ic_credit_card, SupportedPaymentMethod.CREDIT_CARD),
            PaymentMethodItemData(R.drawable.ic_alipay, SupportedPaymentMethod.ALIPAY),
            PaymentMethodItemData(R.mipmap.ic_union_pay, SupportedPaymentMethod.UNION_PAY),
            PaymentMethodItemData(R.mipmap.ic_paypal_logo_200px, SupportedPaymentMethod.PAYPAL),
            PaymentMethodItemData(R.mipmap.ic_linepay, SupportedPaymentMethod.LINE_PAY)
    )
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentMethodItemHolder {
        return PaymentMethodItemHolder(LayoutInflater
                .from(parent.context)
                .inflate(R.layout.payment_method_list_item_view, parent, false))
    }

    override fun getItemCount(): Int = list.count()

    override fun onBindViewHolder(holder: PaymentMethodItemHolder, position: Int) {
        list[position].also { paymentMethodData ->
            holder.imageButton.setImageResource(paymentMethodData.imageRes)

            if (!holder.imageButton.hasOnClickListeners()) {
                holder.imageButton.setOnClickListener {
                    listener.onSelectPaymentMethod(paymentMethodData.method)
                    notifyItemChanged(selectedPosition)
                    selectedPosition = holder.layoutPosition
                    notifyItemChanged(selectedPosition)
                }
            }
        }
        holder.imageButton.isSelected = (position == selectedPosition)
    }

}