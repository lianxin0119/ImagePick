package space.lianxin.img.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/6 16:39
 */
abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: T

    protected abstract fun initViewBinding(): T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = initViewBinding()
        setContentView(binding.root)
        initView()
        initData()
    }

    protected abstract fun initView()

    protected abstract fun initData()

}