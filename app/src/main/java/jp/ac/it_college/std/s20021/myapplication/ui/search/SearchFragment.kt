package jp.ac.it_college.std.s20021.myapplication.ui.search

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import jp.ac.it_college.std.s20021.myapplication.DataClass
import jp.ac.it_college.std.s20021.myapplication.MainActivity
import jp.ac.it_college.std.s20021.myapplication.R
import jp.ac.it_college.std.s20021.myapplication.databinding.FragmentSearchBinding
import java.io.File
import android.widget.Toast
import androidx.core.view.marginTop
import com.google.android.material.snackbar.Snackbar

class SearchFragment : Fragment() {

    private lateinit var searchViewModel: SearchViewModel
    private var _binding: FragmentSearchBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        searchViewModel = ViewModelProvider(this).get(SearchViewModel::class.java)

        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSearch
        searchViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        //DataClassからエンティティリスト呼び出し
        val entitys = DataClass().get_entitys()

        //アプリ内共有データ
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!

        //エンティティ、ポジションを一時保存
        var ent: String = ""
        var ent_pos: Int = sharedPref.getInt(getString(R.string.entitys_position), 0)

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            entitys.keys.toTypedArray()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.typeSpinner.adapter = adapter
        binding.typeSpinner.setSelection(ent_pos)
        binding.typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?,
                view: View?, position: Int, id: Long){
                val spinnerParent = parent as Spinner
                val item = spinnerParent.selectedItem as String
                binding.entityText.text = entitys[item].toString()

                ent_pos = position
                ent = entitys[item].toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        val navController = this.findNavController() //フラグメント遷移に使用
        val searchbutton: Button = binding.searchButtonSearch

        searchbutton.setOnClickListener{
            val searchText = binding.searchView.query.toString().trim()

            if (searchText != "") {
                //検索結果画面に設定
                sharedPref.edit().putString(
                    getString(R.string.display), getString(R.string.display_search)).commit()
                //検索ワードをセット
                sharedPref.edit().putString(
                    getString(R.string.query), searchText).commit()
                //エンティティをセット
                sharedPref.edit().putString(
                    getString(R.string.entity), ent).commit()
                //エンティティリストのポジションをセット
                sharedPref.edit().putInt(
                    getString(R.string.entitys_position), ent_pos).commit()
                navController.navigate(R.id.action_nav_search_to_resultFragment) //リザルトフラグメントへ
            } else {
                /**
                Toast.makeText(
                    activity,
                    "検索する文字を入力してください",
                    Toast.LENGTH_SHORT).show()
                **/
                val snackbar = Snackbar.make( binding.coordinatorLayout,
                    "検索する文字を入力してください",
                    Snackbar.LENGTH_LONG)
                snackbar.show()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}