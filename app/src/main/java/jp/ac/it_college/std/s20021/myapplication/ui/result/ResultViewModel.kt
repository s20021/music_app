package jp.ac.it_college.std.s20021.myapplication.ui.result

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ResultViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is result Fragment"
    }
    val text: LiveData<String> = _text
}