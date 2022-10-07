package space.lianxin.img

import android.widget.Toast
import space.lianxin.image.ImagePick
import space.lianxin.img.base.BaseActivity
import space.lianxin.img.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun initViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        binding.tvHello.setOnClickListener {
            Toast.makeText(this, "12", Toast.LENGTH_SHORT).show()
            ImagePick.build()
                .setCrop(false)
                .useCamera(true)
                .onlyTakePhoto(false)
                .setSingle(false)
                .canPreview(true)
                .setMaxSelectCount(9)
                .setSelected(null)
                .setCropRatio(2f)
                .start(this, 101)
        }

    }

    override fun initData() {}

}