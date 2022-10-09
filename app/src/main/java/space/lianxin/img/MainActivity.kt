package space.lianxin.img

import space.lianxin.image.ImagePick
import space.lianxin.img.base.BaseActivity
import space.lianxin.img.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun initViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        binding.tvHello.setOnClickListener {
            ImagePick.build()
                .setCrop(true)
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