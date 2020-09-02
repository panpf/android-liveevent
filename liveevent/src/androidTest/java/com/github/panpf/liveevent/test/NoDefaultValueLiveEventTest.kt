package com.github.panpf.liveevent.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.github.panpf.liveevent.Listener
import com.github.panpf.liveevent.LiveEvent
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoDefaultValueLiveEventTest {

    @get:Rule
    val activityTestRule = ActivityTestRule(TestActivity::class.java)

    @Test
    fun test() {
        val activity = activityTestRule.activity
        val fragment = activity.fragment

        /*
         * CREATED
         */
        Assert.assertEquals("fragment state error", Lifecycle.State.CREATED, fragment.lifecycle.currentState)
        Assert.assertEquals("listenValue test error", null, fragment.listenValue)
        Assert.assertEquals("listenStickyValue test error", null, fragment.listenStickyValue)
        Assert.assertEquals("listenForeverValue test error", null, fragment.listenForeverValue)
        Assert.assertEquals("listenForeverStickyValue test error", null, fragment.listenForeverStickyValue)

        fragment.viewModel.testLiveEvent.postValue(99);Thread.sleep(100)
        Assert.assertEquals("listenValue test error", null, fragment.listenValue)
        Assert.assertEquals("listenStickyValue test error", null, fragment.listenStickyValue)
        Assert.assertEquals("listenForeverValue test error", 99, fragment.listenForeverValue)
        Assert.assertEquals("listenForeverStickyValue test error", 99, fragment.listenForeverStickyValue)

        /*
         * STARTED
         */
        activity.postMaxLifecycle(Lifecycle.State.STARTED); Thread.sleep(100)
        Assert.assertEquals("fragment state error", Lifecycle.State.STARTED, fragment.lifecycle.currentState)
        Assert.assertEquals("listenValue test error", null, fragment.listenValue)
        Assert.assertEquals("listenStickyValue test error", 99, fragment.listenStickyValue)
        Assert.assertEquals("listenForeverValue test error", 99, fragment.listenForeverValue)
        Assert.assertEquals("listenForeverStickyValue test error", 99, fragment.listenForeverStickyValue)

        fragment.viewModel.testLiveEvent.postValue(109); Thread.sleep(100)
        Assert.assertEquals("listenValue test error", 109, fragment.listenValue)
        Assert.assertEquals("listenStickyValue test error", 109, fragment.listenStickyValue)
        Assert.assertEquals("listenForeverValue test error", 109, fragment.listenForeverValue)
        Assert.assertEquals("listenForeverStickyValue test error", 109, fragment.listenForeverStickyValue)

        /*
         * RESUMED
         */
        activity.postMaxLifecycle(Lifecycle.State.RESUMED); Thread.sleep(100)
        Assert.assertEquals("fragment state error", Lifecycle.State.RESUMED, fragment.lifecycle.currentState)
        Assert.assertEquals("listenStickyValue test error", 109, fragment.listenStickyValue)
        Assert.assertEquals("listenStickyValue test error", 109, fragment.listenStickyValue)
        Assert.assertEquals("listenForeverValue test error", 109, fragment.listenForeverValue)
        Assert.assertEquals("listenForeverStickyValue test error", 109, fragment.listenForeverStickyValue)

        fragment.viewModel.testLiveEvent.postValue(119); Thread.sleep(100)
        Assert.assertEquals("listenStickyValue test error", 119, fragment.listenStickyValue)
        Assert.assertEquals("listenStickyValue test error", 119, fragment.listenStickyValue)
        Assert.assertEquals("listenForeverValue test error", 119, fragment.listenForeverValue)
        Assert.assertEquals("listenForeverStickyValue test error", 119, fragment.listenForeverStickyValue)

        /*
         * STARTED
         */
        activity.postMaxLifecycle(Lifecycle.State.STARTED); Thread.sleep(100)
        Assert.assertEquals("fragment state error", Lifecycle.State.STARTED, fragment.lifecycle.currentState)
        Assert.assertEquals("listenStickyValue test error", 119, fragment.listenStickyValue)
        Assert.assertEquals("listenStickyValue test error", 119, fragment.listenStickyValue)
        Assert.assertEquals("listenForeverValue test error", 119, fragment.listenForeverValue)
        Assert.assertEquals("listenForeverStickyValue test error", 119, fragment.listenForeverStickyValue)

        fragment.viewModel.testLiveEvent.postValue(129); Thread.sleep(100)
        Assert.assertEquals("listenStickyValue test error", 129, fragment.listenStickyValue)
        Assert.assertEquals("listenStickyValue test error", 129, fragment.listenStickyValue)
        Assert.assertEquals("listenForeverValue test error", 129, fragment.listenForeverValue)
        Assert.assertEquals("listenForeverStickyValue test error", 129, fragment.listenForeverStickyValue)

        /*
         * CREATED
         */
        activity.postMaxLifecycle(Lifecycle.State.CREATED); Thread.sleep(100)
        Assert.assertEquals("fragment state error", Lifecycle.State.CREATED, fragment.lifecycle.currentState)
        Assert.assertEquals("listenStickyValue test error", 129, fragment.listenStickyValue)
        Assert.assertEquals("listenStickyValue test error", 129, fragment.listenStickyValue)
        Assert.assertEquals("listenForeverValue test error", 129, fragment.listenForeverValue)
        Assert.assertEquals("listenForeverStickyValue test error", 129, fragment.listenForeverStickyValue)

        fragment.viewModel.testLiveEvent.postValue(139); Thread.sleep(100)
        Assert.assertEquals("listenStickyValue test error", 129, fragment.listenStickyValue)
        Assert.assertEquals("listenStickyValue test error", 129, fragment.listenStickyValue)
        Assert.assertEquals("listenForeverValue test error", 139, fragment.listenForeverValue)
        Assert.assertEquals("listenForeverStickyValue test error", 139, fragment.listenForeverStickyValue)
    }

    class TestActivity : FragmentActivity() {

        val fragment = TestFragment()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .setMaxLifecycle(fragment, Lifecycle.State.CREATED)
                    .commit()
        }

        fun postMaxLifecycle(state: Lifecycle.State) {
            supportFragmentManager.beginTransaction()
                    .setMaxLifecycle(fragment, state)
                    .commit()
        }
    }

    class TestViewModel : ViewModel() {
        val testLiveEvent = LiveEvent<Int>()
    }

    class TestFragment : Fragment() {

        val viewModel by lazy { ViewModelProvider(this, defaultViewModelProviderFactory).get(TestViewModel::class.java) }

        var listenValue: Int? = null
        var listenStickyValue: Int? = null
        var listenForeverValue: Int? = null
        var listenForeverStickyValue: Int? = null

        private val listenForeverListener = Listener<Int> {
            listenForeverValue = it
        }

        private val listenForeverStickyListener = Listener<Int> {
            listenForeverStickyValue = it
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            viewModel.testLiveEvent.listenForever(listenForeverListener)
            viewModel.testLiveEvent.listenForeverSticky(listenForeverStickyListener)
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return FrameLayout(requireContext())
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            viewModel.testLiveEvent.listen(viewLifecycleOwner, Listener {
                listenValue = it
            })

            viewModel.testLiveEvent.listenSticky(viewLifecycleOwner, Listener {
                listenStickyValue = it
            })
        }

        override fun onDestroy() {
            viewModel.testLiveEvent.removeListener(listenForeverListener)
            viewModel.testLiveEvent.removeListener(listenForeverStickyListener)
            super.onDestroy()
        }
    }
}