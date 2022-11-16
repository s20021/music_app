package jp.ac.it_college.std.s20021.myapplication.ui.result

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.ContactsContract.Data
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.HandlerCompat.postDelayed
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import jp.ac.it_college.std.s20021.myapplication.R
import jp.ac.it_college.std.s20021.myapplication.DataClass
import jp.ac.it_college.std.s20021.myapplication.MainActivity
import jp.ac.it_college.std.s20021.myapplication.databinding.*
import jp.ac.it_college.std.s20021.myapplication.ui.home.HomeViewModel
import jp.ac.it_college.std.s20021.myapplication.ui.search.SearchViewModel
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.Future

/** 初期設定
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
**/


/**
 * A simple [Fragment] subclass.
 * Use the [ResultFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class ResultFragment : Fragment() {
    /** 初期
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    **/

    private lateinit var resultViewModel: ResultViewModel
    private var _binding: FragmentResultBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    /** 初期
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }
    **/

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        resultViewModel = ViewModelProvider(this).get(ResultViewModel::class.java)

        _binding = FragmentResultBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //ボタン非表示
        binding.jsonBackButton.visibility = View.INVISIBLE
        binding.jsonForwardButton.visibility = View.INVISIBLE

        //アプリ内共有データ
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!

        val defaultLimit = DataClass().get_displayed_default()

        var entity = sharedPref.getString(getString(R.string.entity), "").toString()
        var query = sharedPref.getString(getString(R.string.query), " ").toString()
        var limit = sharedPref.getInt(getString(R.string.limit), defaultLimit)
        var offset = sharedPref.getInt(getString(R.string.offset), 0)
        var mb_api_url = DataClass().get_url_search(entity, query, limit, offset)

        var display_mode = sharedPref.getString(getString(R.string.display), "").toString()

        val test_txt: TextView = binding.resultTestText
        val test_txt2: TextView = binding.resultTestText2
        test_txt.text = mb_api_url
        test_txt2.text = "null"

        //戻るボタン設定
        binding.jsonBackButton.setOnClickListener {
            limit = sharedPref.getInt(getString(R.string.limit), limit)
            offset = sharedPref.getInt(getString(R.string.offset), offset)
            val dis_res_num = sharedPref.getInt(getString(R.string.displayed_result_num), DataClass().get_displayed_default())
            if (offset - dis_res_num < 0) { offset = 0 }
            else { offset = offset - dis_res_num }
            //ボタン無効化
            binding.jsonBackButton.isEnabled = false
            binding.jsonForwardButton.isEnabled = false

            mb_api_url = DataClass().get_url_search(entity, query, limit, offset)
            test_txt.text = mb_api_url
            Handler().postDelayed(
                { apiTask(mb_api_url, display_mode) }, 1000)
        }

        //進むボタン設定
        binding.jsonForwardButton.setOnClickListener {
            val dis_res_num = sharedPref.getInt(getString(R.string.displayed_result_num), DataClass().get_displayed_default())
            limit = sharedPref.getInt(getString(R.string.limit), limit)
            offset = sharedPref.getInt(getString(R.string.offset), offset) + dis_res_num
            //ボタン無効化
            binding.jsonBackButton.isEnabled = false
            binding.jsonForwardButton.isEnabled = false

            mb_api_url = DataClass().get_url_search(entity, query, limit, offset)
            test_txt.text = mb_api_url
            Handler().postDelayed(
                { apiTask(mb_api_url, display_mode) }, 1000)
        }

        //api通信 & 作業
        apiTask(mb_api_url, display_mode)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        //設定したURLを削除
        apiUrlUnSet()
    }

    /**
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ResultFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ResultFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    **/

    //URLを保存、削除
    fun apiUrlSet(url: String) {
        activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!
            .edit().putString(getString(R.string.API_URL), url)
    }
    fun apiUrlUnSet() {
        activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!
            .edit().putString(getString(R.string.API_URL), "")
    }

    //apiの取得、処理
    private fun apiTask(mburl: String, display_str: String, limit: Int = 0) {
        binding.progressBar.visibility = android.widget.ProgressBar.VISIBLE
        apiUrlSet(mburl)
        lifecycleScope.launch {
            val result = apiBackGroundTask(mburl)
            if (display_str == getString(R.string.display_search)) {
                apiJsonTaskSearch(result, limit) }
            else if (display_str == getString(R.string.display_lookup)) {
                apiJsonTaskLookup(result)
            }
            binding.progressBar.visibility = android.widget.ProgressBar.INVISIBLE
        }
    }

    private suspend fun apiBackGroundTask(mburl: String): String {
        val response = withContext(Dispatchers.IO){
            var httpResult = ""

            try {
                val urlObj = URL(mburl)
                val br = BufferedReader(InputStreamReader(urlObj.openStream()))

                //httpResult = br.toString()
                httpResult = br.readText()
            }catch (e: MalformedURLException){
                e.printStackTrace()
            }catch (e:IOException){
                e.printStackTrace()
            }catch (e:JSONException){
                e.printStackTrace()
            }
            return@withContext httpResult
        }
        return response
    }

    //検索結果
    private fun apiJsonTaskSearch(result: String, limit: Int){
        //戻る、進むボタン
        val backButton = binding.jsonBackButton
        val forwardButton = binding.jsonForwardButton

        if (result == "") {
            Toast.makeText(requireContext(), "JSONを取得できませんでした", Toast.LENGTH_LONG).show()
            return
        }
        //共有データ
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!

        //受け取ったデータをjsonオブジェクトに
        val jsonObj = JSONObject(result)

        //jsonからオフセット、カウント
        val json_offset: Int = jsonObj.getInt("offset")
        val json_count: Int = jsonObj.getInt("count")

        //jsonのkeyをリストに
        val jsonKeys: MutableList<String> = mutableListOf<String>()
        var keys_num = 0
        for (i in jsonObj.keys()) {
            jsonKeys.add(i.toString())
            keys_num += 1}
        keys_num -= 1

        //受け取ったリストを変換
        val searchResultList = jsonObj.getJSONArray(jsonKeys[keys_num])
        //結果リスト生成
        val resultList: MutableList<JSONObject> = mutableListOf<JSONObject>()
        val listLength: Int = searchResultList.length()
        for (i in 0..listLength - 1) {
            resultList.add(searchResultList.getJSONObject(i))
        }

        fun listSet(slist: MutableList<String>, gkey: String) {
            for (i in resultList) {
                var resultKeys = mutableListOf<String>()
                for (k in i.keys()) { resultKeys.add(k.toString()) }

                if (resultKeys.contains(gkey)) {
                    slist.add(i[gkey].toString())
                } else {
                    slist.add("")
                }
            }
        }

        fun scoreListSet(sList: MutableList<String>, gkey: String = "score") {
            for (i in resultList) {
                val resultKeys = mutableListOf<String>()
                for (k in i.keys()) { resultKeys.add(k.toString()) }

                if (resultKeys.contains(gkey)) {
                    sList.add(i[gkey].toString().padStart(3, '0'))
                } else {
                    sList.add("".padStart(3, ' '))
                }
            }
        }

        fun areaListSet(slist: MutableList<String>, gkey: String = "area") {
            for (i in resultList) {
                var resultKeys = mutableListOf<String>()
                for (k in i.keys()) { resultKeys.add(k.toString()) }

                if (resultKeys.contains(gkey)) {
                    val api_area = i.getJSONObject(gkey)
                    slist.add(api_area["name"].toString())
                } else {
                    slist.add("")
                }
            }
        }

        //"name"list生成
        val nameList: MutableList<String> = mutableListOf()
        listSet(nameList, "name")
        //"title"list生成
        val titleList: MutableList<String> = mutableListOf()
        listSet(titleList, "title")

        //"score"list生成
        val scoreList: MutableList<String> = mutableListOf()
        scoreListSet(scoreList)

        //"id"list生成
        val mbidList: MutableList<String> = mutableListOf()
        listSet(mbidList, "id")
        //"country"list生成
        val countryList: MutableList<String> = mutableListOf()
        listSet(countryList, "country")
        //"area"list生成
        val areaList: MutableList<String> = mutableListOf()
        areaListSet(areaList)

        val displayResultList: MutableList<String> = mutableListOf()
        for (i in 0 .. listLength - 1) {
            var name = nameList[i].trim()
            val score = scoreList[i].trim()
            val country = countryList[i].trim()
            val area = areaList[i].trim()
            if (name == "") { name = titleList[i].trim() }

            if (area != "") {
                displayResultList.add("$score neme: 「${name}」   area: ${area}")
            } else if (country != "") {
                displayResultList.add("$score neme: 「${name}」   country: ${country}")
            } else {
                displayResultList.add("$score name: 「${name}」")
            }
        }

        //件数表示
        binding.resultTestText2.text =
            "計${json_count}件中　${json_offset + 1}～${json_offset + listLength}件まで表示中　表示件数:${listLength}"

        //ボタン無効化
        backButton.isClickable = false
        forwardButton.isClickable = false
        //ボタン表示
        backButton.visibility = View.VISIBLE
        forwardButton.visibility = View.VISIBLE

        if (json_offset <= 0) {
            //戻るボタンの色変化
            backButton.isEnabled = false }
        else {
            //戻るボタン有効化
            backButton.isEnabled = true
            backButton.isClickable = true }

        if (json_count <= json_offset + listLength) {
            //進むボタンの色変化
            forwardButton.isEnabled = false }
        else {
            //進むボタン有効化
            forwardButton.isEnabled = true
            forwardButton.isClickable = true }

        //listviewを設定
        val listView: ListView = binding.apiResultList
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            displayResultList
        )
        listView.adapter = adapter
        listView.isEnabled = true

        listView.setOnItemClickListener { adapterView, view, position, id ->
            listView.isEnabled = false
            binding.resultTestText2.text = mbidList[position]
            sharedPref.edit().putString(getString(R.string.API_URL), mbidList[position])
        }
    }

    //アーティスト、楽曲情報
    private fun apiJsonTaskLookup(result: String) {
        //進む、戻るボタン非表示
        binding.jsonBackButton.visibility = View.INVISIBLE
        binding.jsonForwardButton.visibility = View.INVISIBLE

        if (result == "") {
            Toast.makeText(requireContext(), "JSONを取得できませんでした", Toast.LENGTH_LONG).show()
            return
        }

        //受け取ったデータをjsonオブジェクトに
        val jsonObj = JSONObject(result)
    }
}