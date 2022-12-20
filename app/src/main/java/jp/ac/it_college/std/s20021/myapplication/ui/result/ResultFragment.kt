package jp.ac.it_college.std.s20021.myapplication.ui.result

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import jp.ac.it_college.std.s20021.myapplication.R
import jp.ac.it_college.std.s20021.myapplication.DataClass
import jp.ac.it_college.std.s20021.myapplication.databinding.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL

class ResultFragment : Fragment() {

    private lateinit var resultViewModel: ResultViewModel
    private var _binding: FragmentResultBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!



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
        binding.floatingActionButton.visibility = View.INVISIBLE
        binding.historyAddButton.visibility = View.INVISIBLE

        //アプリ内共有データ
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!

        var entity = sharedPref.getString(getString(R.string.entity), "").toString()
        var query = sharedPref.getString(getString(R.string.query), " ").toString()
        var limit = sharedPref.getInt(getString(R.string.limit), DataClass().get_displayed_default())
        var offset = sharedPref.getInt(getString(R.string.offset), 0)
        var mb_api_url = DataClass().get_url_search(entity, query, limit, offset)

        var display_mode = sharedPref.getString(getString(R.string.display), "").toString()

        var dis_res_num = sharedPref.getInt(getString(R.string.displayed_result_num), DataClass().get_displayed_default())

        fun sharedPrefReload() {
            entity = sharedPref.getString(getString(R.string.entity), "").toString()
            query = sharedPref.getString(getString(R.string.query), " ").toString()
            limit = sharedPref.getInt(getString(R.string.limit), DataClass().get_displayed_default())
            offset = sharedPref.getInt(getString(R.string.offset), 0)

            display_mode = sharedPref.getString(getString(R.string.display), "").toString()

            dis_res_num = sharedPref.getInt(getString(R.string.displayed_result_num), DataClass().get_displayed_default())
        }

        fun limOffReload() {
            if (limit != dis_res_num) {
                limit = dis_res_num
                sharedPref.edit().putInt(getString(R.string.limit), dis_res_num)
            }
            offset = sharedPref.getInt(getString(R.string.offset), offset)
        }

        //戻るボタン設定
        binding.jsonBackButton.setOnClickListener {
            //アプリ内共有関数 limit offset 再読み込み
            limOffReload()
            if (offset - dis_res_num < 0) { offset = 0 }
            else { offset = offset - dis_res_num }

            //ボタン無効化
            binding.jsonBackButton.isEnabled = false
            binding.jsonForwardButton.isEnabled = false

            mb_api_url = DataClass().get_url_search(entity, query, limit, offset)
            //一秒待ち
            Handler().postDelayed(
                { apiTask(mb_api_url, display_mode, entity) }, 1000)
        }

        //進むボタン設定
        binding.jsonForwardButton.setOnClickListener {
            //アプリ内共有関数 limit offset 再読み込み
            limOffReload()
            offset += dis_res_num

            //offset保存
            sharedPref.edit().putInt(getString(R.string.offset), offset)

            //ボタン無効化
            binding.jsonBackButton.isEnabled = false
            binding.jsonForwardButton.isEnabled = false

            mb_api_url = DataClass().get_url_search(entity, query, limit, offset)
            //一秒待ち
            Handler().postDelayed(
                { apiTask(mb_api_url, display_mode, entity) }, 1000)
        }

        binding.floatingActionButton.setOnClickListener {
            val historyList = mutableMapOf<String, String>()
            historyList.put(getString(R.string.API_URL), mb_api_url)
        }

        //api通信 & 作業
        apiTask(mb_api_url, display_mode, entity)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        //設定したURLを削除
        apiUrlUnSet()
    }

//---------------------------------------------------------------------------------------------------
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


//---------------------------------------------------------------------------------------------------
    //apiの取得、処理
    private fun apiTask(mburl: String, display_str: String, entity: String) {
        binding.progressBar.visibility = android.widget.ProgressBar.VISIBLE
        apiUrlSet(mburl) //url一時保存

        lifecycleScope.launch {
            val result = apiBackGroundTask(mburl)
            if (display_str == getString(R.string.display_search)) {
                if (entity == "1artist") { apiJsonTaskSearchArtist(result)
                //} else if (entity == "release") { apiJsonTaskSearchRelease(result)
                //} else if (entity == "recording") { apiJsonTaskSearchRecording(result)
                //} else if (entity == "event") { apiJsonTaskSearch(result)
                } else { apiJsonTaskSearchTest(result) }
            } else if (display_str == getString(R.string.display_lookup)) {
                if (entity == "artist") { apiJsonTaskLookupArtist(result)
                //} else if (entity == "release") { apiJsonTaskLookupRelease(result)
                } else { apiJsonTaskLookupTest(result) }
            }
            binding.progressBar.visibility = android.widget.ProgressBar.INVISIBLE
        }
    }

    //jsonの取得
    private suspend fun apiBackGroundTask(mburl: String): String {
        binding.resultText.text = mburl

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
        Handler().postDelayed(
            {  }, 1000)
        return response
    }

    //検索結果
    //アーティスト
    private fun apiJsonTaskSearchArtist(result: String) {
        //エラーチェック
        if (result == "") {
            errorToast().show()
            return
        }

        //受け取ったデータをjsonオブジェクトに
        val jsonObj = JSONObject(result)

        //jsonのkeyをリストに
        val jsonKeys: MutableList<String> = mutableListOf<String>()
        var keys_num = 0
        for (i in jsonObj.keys()) {
            jsonKeys.add(i.toString())
            keys_num += 1}
        keys_num -= 1

        //エラーチェック
        if (jsonKeys.contains("error") || jsonKeys.contains("help")) {
            errorToast().show()
            return
        }

        //共有データ
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!

        //jsonからオフセット、カウント
        val json_offset: Int = jsonObj.getInt("offset")
        val json_count: Int = jsonObj.getInt("count")

        //受け取ったリストを変換
        val searchResultList = jsonObj.getJSONArray(jsonKeys[keys_num])
        //結果リスト生成
        val resultList: MutableList<JSONObject> = mutableListOf<JSONObject>()
        val listLength: Int = searchResultList.length()
        for (i in 0..listLength - 1) {
            resultList.add(searchResultList.getJSONObject(i))
        }

        //件数表示
        if (json_count <= 0) {
            binding.resultText.text = "見つかりませんでした"
        } else if (json_count > 0) {
            binding.resultText.text =
                "計${json_count}件中　${json_offset + 1}～${json_offset + listLength}件まで表示中　表示件数:${listLength}"
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

        //"score"list生成
        val scoreList: MutableList<String> = mutableListOf()
        scoreListSet(scoreList)

        //"id"list生成
        val mbidList: MutableList<String> = mutableListOf()
        listSet(mbidList, "id")

        //"type"list生成
        val typeList: MutableList<String> = mutableListOf()
        listSet(typeList, "type")
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
            displayResultList.add("$score : 「 $name 」")
        }

        //ボタンの表示設定
        searchForBacButtonSetting(json_count, json_offset, listLength)

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
            binding.resultText.text = mbidList[position]

            fun mbapiLookup() {
                val lookUpUrl = DataClass().get_url_RelRels(
                    sharedPref.getString(getString(R.string.entity), "").toString(),
                    mbidList[position]
                )
                sharedPref.edit().putString(
                    getString(R.string.display),
                    getString(R.string.display_lookup)
                ).commit()
                apiTask(
                    lookUpUrl,
                    sharedPref.getString(getString(R.string.display), "").toString(),
                    "artist"
                )
            }

            var artistUrlelsURL: String? = null

            var relationsType: MutableList<String>? = null
            var relationsURL: MutableList<String>? = null

            val resultDialog: AlertDialog = activity.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setTitle(nameList[position] + " の情報")
                    setMessage(
                        "\n" +
                        "score    "  + " :  " + "${scoreList[position]}\n" +
                        "type      " + " :  " + "${typeList[position]}\n" +
                        "country"    + " :  " + "${countryList[position]}\n" +
                        "area      " + " :  " + "${areaList[position]}\n"
                    )
                    setPositiveButton("リリース情報を表示", { dialog, id ->
                        mbapiLookup()
                    })
                    setNegativeButton("URLを共有する", { dialog, id ->
                        val urltmp = DataClass().get_url_urlels("artist", mbidList[position])

                        fun artistRelationsTask() {
                            lifecycleScope.launch {
                                val result = apiBackGroundTask(artistUrlelsURL!!)

                                val relationsList = geturlels(result)
                                if (relationsList != null) {
                                    relationsType = getRelType(relationsList)
                                    relationsURL = getRelUrl(relationsList)
                                    val relationsURLText = URLTextSetting(relationsURL!!, relationsType!!)
                                    Handler().postDelayed({
                                        shareDialogShow(nameList[position], relationsURL!!, relationsType!!, relationsURLText)
                                                          }, 1000)
                                } else {
                                    errorToast()
                                }
                            }
                        }

                        if (artistUrlelsURL == null) {
                            artistUrlelsURL = urltmp
                            artistRelationsTask()
                        } else if (artistUrlelsURL != urltmp) {
                            artistUrlelsURL = urltmp
                            artistRelationsTask()
                        } else if (relationsURL != null && relationsType != null) {
                            shareDialogShow(nameList[position], relationsURL!!, relationsType!!)
                        }
                    })
                    setNeutralButton("キャンセル", { dialog, id ->
                    })
                }
                listView.isEnabled = true
                builder.create()
            }
            resultDialog.show()
        }
    }
    //リリース
    private fun apiJsonTaskSearchRelease(result: String) {
        //エラーチェック
        if (result == "") {
            errorToast().show()
            return
        }

        //受け取ったデータをjsonオブジェクトに
        val jsonObj = JSONObject(result)

        //jsonのkeyをリストに
        val jsonKeys: MutableList<String> = mutableListOf<String>()
        var keys_num = 0
        for (i in jsonObj.keys()) {
            jsonKeys.add(i.toString())
            keys_num += 1}
        keys_num -= 1

        //エラーチェック
        if (jsonKeys.contains("error") || jsonKeys.contains("help")) {
            errorToast().show()
            return
        }

        //共有データ
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!

        //jsonからオフセット、カウント
        val json_offset: Int = jsonObj.getInt("offset")
        val json_count: Int = jsonObj.getInt("count")

        //受け取ったリストを変換
        val searchResultList = jsonObj.getJSONArray(jsonKeys[keys_num])
        //結果リスト生成
        val resultList: MutableList<JSONObject> = mutableListOf<JSONObject>()
        val listLength: Int = searchResultList.length()
        for (i in 0..listLength - 1) {
            resultList.add(searchResultList.getJSONObject(i))
        }

        //件数表示
        if (json_count <= 0) {
            binding.resultText.text = "見つかりませんでした"
        } else if (json_count > 0) {
            binding.resultText.text =
                "計${json_count}件中　${json_offset + 1}～${json_offset + listLength}件まで表示中　表示件数:${listLength}"
        }
    }
    //レコーディング
    private fun apiJsonTaskSearchRecording(result: String) {
        //エラーチェック
        if (result == "") {
            errorToast().show()
            return
        }

        //受け取ったデータをjsonオブジェクトに
        val jsonObj = JSONObject(result)

        //jsonのkeyをリストに
        val jsonKeys: MutableList<String> = mutableListOf<String>()
        var keys_num = 0
        for (i in jsonObj.keys()) {
            jsonKeys.add(i.toString())
            keys_num += 1}
        keys_num -= 1

        //エラーチェック
        if (jsonKeys.contains("error") || jsonKeys.contains("help")) {
            errorToast().show()
            return
        }

        //共有データ
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!

        //jsonからオフセット、カウント
        val json_offset: Int = jsonObj.getInt("offset")
        val json_count: Int = jsonObj.getInt("count")

        //受け取ったリストを変換
        val searchResultList = jsonObj.getJSONArray(jsonKeys[keys_num])
        //結果リスト生成
        val resultList: MutableList<JSONObject> = mutableListOf<JSONObject>()
        val listLength: Int = searchResultList.length()
        for (i in 0..listLength - 1) {
            resultList.add(searchResultList.getJSONObject(i))
        }

        //件数表示
        if (json_count <= 0) {
            binding.resultText.text = "見つかりませんでした"
        } else if (json_count > 0) {
            binding.resultText.text =
                "計${json_count}件中　${json_offset + 1}～${json_offset + listLength}件まで表示中　表示件数:${listLength}"
        }
    }

    //戻る、進むボタンの表示・非表示設定
    private fun searchForBacButtonSetting(json_count: Int, json_offset: Int, resultLength: Int) {
        //戻る、進むボタン
        val backButton = binding.jsonBackButton
        val forwardButton = binding.jsonForwardButton

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

        if (json_count <= json_offset + resultLength) {
            //進むボタンの色変化
            forwardButton.isEnabled = false }
        else {
            //進むボタン有効化
            forwardButton.isEnabled = true
            forwardButton.isClickable = true }
    }

    //Test
    private fun apiJsonTaskSearchTest(result: String) {
        //戻る、進むボタン
        val backButton = binding.jsonBackButton
        val forwardButton = binding.jsonForwardButton

        //エラーチェック
        if (result == "") {
            errorToast().show()
            return
        }
        //共有データ
        val sharedPref = activity?.getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)!!

        //受け取ったデータをjsonオブジェクトに
        val jsonObj = JSONObject(result)

        //jsonのkeyをリストに
        val jsonKeys: MutableList<String> = mutableListOf<String>()
        var keys_num = 0
        for (i in jsonObj.keys()) {
            jsonKeys.add(i.toString())
            keys_num += 1}
        keys_num -= 1

        //エラーチェック
        if (jsonKeys.contains("error") || jsonKeys.contains("help")) {
            errorToast().show()
            return
        }


        //jsonからオフセット、カウント
        val json_offset: Int = jsonObj.getInt("offset")
        val json_count: Int = jsonObj.getInt("count")

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
        if (json_count <= 0) {
            binding.resultText.text = "見つかりませんでした"
        } else if (json_count > 0) {
            binding.resultText.text =
                "計${json_count}件中　${json_offset + 1}～${json_offset + listLength}件まで表示中　表示件数:${listLength}"
        }

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
            binding.resultText.text = mbidList[position]
            val lookUpUrl = DataClass().get_url_RelRels(
                sharedPref.getString(getString(R.string.entity), "").toString(),
                mbidList[position]
            )
            sharedPref.edit().putString(
                getString(R.string.display),
                getString(R.string.display_lookup)
            ).commit()
            apiTask(
                lookUpUrl,
                sharedPref.getString(getString(R.string.display), "").toString(),
                ""
            )
        }
    }

//---------------------------------------------------------------------------------------------------
    //アーティスト、楽曲情報
    private fun apiJsonTaskLookupArtist(result: String) {
        //進む、戻るボタン非表示
        binding.jsonBackButton.visibility = View.INVISIBLE
        binding.jsonForwardButton.visibility = View.INVISIBLE

        binding.floatingActionButton.visibility = View.VISIBLE

        //エラーチェック
        if (result == "") {
            errorToast().show()
            return
        }

        //受け取ったデータをjsonオブジェクトに
        val jsonObj = JSONObject(result)

        //jsonのkeyをリストに
        val jsonKeys: MutableList<String> = mutableListOf<String>()
        for (i in jsonObj.keys()) {
            jsonKeys.add(i.toString())
        }
        //エラーチェック
        if (jsonKeys.contains("error")) {
            errorToast().show()
            return
        }

        //urlを取得
        val relationsList = jsonObj.getJSONArray("relations")

        val relationsType = mutableListOf<String>()
        for (i in 0..relationsList.length()-1) {
            relationsType.add(
                relationsList.getJSONObject(i).getString("type")
            )
        }
        val relationsUrl = mutableListOf<String>()
        for (i in 0..relationsList.length()-1) {
            relationsUrl.add(
                relationsList.getJSONObject(i).getJSONObject("url").getString("resource")
            )
        }

        val relationsUrlText = URLTextSetting(relationsUrl, relationsType)

        //シェアボタンを設定
        binding.floatingActionButton.setOnClickListener{
            shareDialogShow(jsonObj.getString("name"), relationsUrl, relationsType, relationsUrlText)
        }

        //release取得
        val releasesList = jsonObj.getJSONArray("releases")

        fun releaseGetStringItem(release: JSONObject, str: String): String {
            for (k in release.keys()) {
                if (k.toString() == str) {
                    return release.getString(str).toString()
                }
            }
            return ""
        }

        val releaseIdList = mutableListOf<String>()
        val releaseTitleList = mutableListOf<String>()
        val releaseDateList = mutableListOf<String>()
        val releaseBarcodeList = mutableListOf<String>()
        val releaseCountryList = mutableListOf<String>()
        for (i in 0..releasesList.length()-1) {
            releaseTitleList.add(
                releaseGetStringItem(releasesList.getJSONObject(i), "title"))
            releaseIdList.add(
                releaseGetStringItem(releasesList.getJSONObject(i), "id"))
            releaseDateList.add(
                releaseGetStringItem(releasesList.getJSONObject(i), "date"))
            releaseBarcodeList.add(
                releaseGetStringItem(releasesList.getJSONObject(i), "barcode"))
            releaseCountryList.add(
                releaseGetStringItem(releasesList.getJSONObject(i), "country"))
        }

        //listviewを設定
        val listView: ListView = binding.apiResultList
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            releaseTitleList
        )
        listView.adapter = adapter
        listView.isEnabled = true

        //リストタップ時の処理
        listView.setOnItemClickListener { adapterView, view, position, id ->
            //リストのタップ不可
            listView.isEnabled = false
            binding.resultText.text = releaseIdList[position]

            val resultDialog: AlertDialog = activity.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setTitle("${jsonObj.getString("name")}")
                    setMessage(
                        "\n" +
                                "title     " + " :  " + "${releaseTitleList[position]}\n" +
                                "date      "  + " :  " + "${releaseDateList[position]}\n" +
                                "country"    + " :  " + "${releaseCountryList[position]}\n" +
                                "barcode" + " :  " + "${releaseBarcodeList[position]}\n"
                    )
                    setPositiveButton(getString(R.string.share_button_text), { dialog, id ->
                        val url = DataClass().get_url_urlels("release", releaseIdList[position])

                        fun releaseRelationsTask() {
                            lifecycleScope.launch {
                                val result = apiBackGroundTask(url)

                                val relationsList = geturlels(result)
                                if (relationsList != null) {
                                    val relationsTypes = getRelType(relationsList)
                                    val relationsURLs = getRelUrl(relationsList)
                                    val relationsURLTexts = URLTextSetting(relationsURLs, relationsTypes)
                                    Handler().postDelayed({
                                        shareDialogShow(
                                            releaseTitleList[position],
                                            relationsURLs,
                                            relationsTypes,
                                            relationsURLTexts
                                        )
                                    }, 1000)
                                } else {
                                    Handler().postDelayed({
                                        errorToast().show()
                                    }, 1000)
                                }
                            }
                        }
                        releaseRelationsTask()
                    })
                    setNegativeButton(getString(R.string.cancel), { dialog, id ->
                    })
                }
                listView.isEnabled = true
                builder.create()
            }
            resultDialog.show()

            val relnum = relationsList.length()
            val checkedItems = mutableListOf<Boolean>()
            for (n in 1..relnum) {
                checkedItems.add(true)
            }
        }
    }

    private fun apiJsonTaskLookupRelease(result: String) {
    }

    private fun apiJsonTaskLookUpRecording(result: String) {
    }

    private fun apiJsonTaskLookupTest(result: String) {
        //進む、戻るボタン非表示
        binding.jsonBackButton.visibility = View.INVISIBLE
        binding.jsonForwardButton.visibility = View.INVISIBLE

        binding.floatingActionButton.visibility = View.VISIBLE

        //エラー通知
        val errorToast = Toast.makeText(requireContext(), "JSONを取得できませんでした", Toast.LENGTH_LONG)
        //エラーチェック
        if (result == "") {
            errorToast.show()
            return
        }

        //受け取ったデータをjsonオブジェクトに
        val jsonObj = JSONObject(result)

        //jsonのkeyをリストに
        val jsonKeys: MutableList<String> = mutableListOf<String>()
        for (i in jsonObj.keys()) {
            jsonKeys.add(i.toString())
        }
        //エラーチェック
        if (jsonKeys.contains("error")) {
            errorToast.show()
            return
        }

        //urlを取得
        val relationsList = jsonObj.getJSONArray("relations")

        val relationsName = mutableListOf<String>()
        for (i in 0..relationsList.length()-1) {
            relationsName.add(
                relationsList.getJSONObject(i).getString("type")
            )
        }
        val relationsUrl = mutableListOf<String>()
        for (i in 0..relationsList.length()-1) {
            relationsUrl.add(
                relationsList.getJSONObject(i).getJSONObject("url").getString("resource")
            )
        }

        //シェアボタンを設定
        binding.floatingActionButton.setOnClickListener{
            shareDialogShow(jsonObj.getString("name"), relationsUrl, relationsName)
        }

        //release取得
        val releasesList = jsonObj.getJSONArray("releases")

        val releaseIdList = mutableListOf<String>()
        val releaseTitleList = mutableListOf<String>()
        val relbools = mutableListOf<Boolean>()
        for (i in 0..releasesList.length()-1) {
            releaseTitleList.add(
                releasesList.getJSONObject(i).getString("title").toString())
            releaseIdList.add(
                releasesList.getJSONObject(i).getString("id").toString())
            relbools.add(true)
        }

        //listviewを設定
        val listView: ListView = binding.apiResultList
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            releaseTitleList
        )
        listView.adapter = adapter
        listView.isEnabled = true

        listView.setOnItemClickListener { adapterView, view, position, id ->
            //リストのタップ不可
            listView.isEnabled = false
            binding.resultText.text = releaseIdList[position]

            val bools = mutableListOf<Boolean>()
            for (i in 1..relationsList.length()) { bools.add(false) }
            val multiChoiceBools = bools.toBooleanArray()

            //ダイアログ生成
            /**
            val dialog: AlertDialog = activity.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setTitle("")
                    setMultiChoiceItems(relationsUrl.toTypedArray(), null, { dialog, which, isChecked ->
                        multiChoiceBools[which] = !multiChoiceBools[which]
                    })
                    setPositiveButton("共有", { dialog, id ->
                        //共有する文字列
                        var shareText = jsonObj.getString("name") + "\n"
                        for (i in 0..relationsList.length()-1) {
                            if (multiChoiceBools[i]){
                                shareText = shareText + "\n${relationsName[i]}: ${relationsUrl[i]}"
                            }
                        }
                        val share: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        startActivity(share)
                    })
                    setNegativeButton("戻る", { dialog, id ->
                    })
                }
                listView.isEnabled = true
                builder.create()
            }
            dialog.show()
            */

            shareDialogShow(jsonObj.getString("name"), relationsUrl, relationsName)

            val relnum = relationsList.length()
            val checkedItems = mutableListOf<Boolean>()
            for (n in 1..relnum) {
                checkedItems.add(true)
            }
        }
    }

//----------------------------------------------------------------------------------------------
    fun errorToast(): Toast {
        return Toast.makeText(requireContext(), getString(R.string.jsonErrorMessage), Toast.LENGTH_LONG)
    }

    private fun geturlels(result: String): JSONArray? {
        //エラーチェック
        if (result == "") {
            return null
        }

        //受け取ったデータをjsonオブジェクトに
        val jsonObj = JSONObject(result)

        //jsonのkeyをリストに
        val jsonKeys: MutableList<String> = mutableListOf<String>()
        for (i in jsonObj.keys()) {
            jsonKeys.add(i.toString())
        }

        //エラーチェック
        if (jsonKeys.contains("error") || jsonKeys.contains("help")) {
            return null
        }


        val relationsList = jsonObj.getJSONArray("relations")

        if (relationsList.length() > 0) { return relationsList }

        return null
    }

    private fun getRelType(relList: JSONArray): MutableList<String> {
        val relationsName = mutableListOf<String>()
        for (i in 0..relList.length()-1) {
            relationsName.add(
                relList.getJSONObject(i).getString("type")
            )
        }
        return relationsName
    }

    private fun getRelUrl(relList: JSONArray): MutableList<String> {
        val relationsUrl = mutableListOf<String>()
        for (i in 0..relList.length()-1) {
            relationsUrl.add(
                relList.getJSONObject(i).getJSONObject("url").getString("resource")
            )
        }
        return relationsUrl
    }

    fun URLSetting(urllist: MutableList<String>) {
        val urlTextList = mutableListOf<String>()
        for (url in urllist) {
            var num: Int = 0
            for (i in 0..url.length-1) {
                if (url[i] == '/') {
                    num = i
                    break
                }
            }
            val urlTextCut = url.substring(num, url.length)
            for (i in 0 .. urlTextCut.length-1) {

            }
            urlTextList.add("")
        }
    }

    fun URLTextSetting(urllist: MutableList<String>, urltypelist: MutableList<String>): MutableList<String> {
        val urlTextList = mutableListOf<String>()

        for (n in 0..urllist.size-1) {
            val url = urllist[n]
            var omitURL: String
            var num: Int = 0
            for (i in 0..url.length-1) {
                if (url[i] == '/') {
                    num = i
                    if (8 > i && url[i + 1] == '/') {
                        num = i+2
                    }
                    break
                }
            }

            val httpCutURL = url.substring(num)
            num = httpCutURL.length
            var count: Int = 0
            for (i in 0 .. httpCutURL.length-1) {
                if (httpCutURL[i] == '/') { count++ }
                if (count >= 2) {
                    num = i+1
                    break
                }
            }
            omitURL = httpCutURL.substring(0, num)
            val urltype = urltypelist[n]
            if (count <= 1) {
                urlTextList.add("[$urltype]\n$omitURL")
            } else {
                urlTextList.add("[$urltype]\n${omitURL + "..."}")
            }
        }
        return urlTextList
    }

    private fun shareDialogShow(shareHeader: String, urlList: MutableList<String>, urlTypeList: MutableList<String>, urlTextList: MutableList<String> = urlList) {
        val resultDialog: AlertDialog = activity.let {
            val builder = AlertDialog.Builder(it)

            val bools = mutableListOf<Boolean>()
            for (n in urlList) { bools.add(false) }
            val multiChoiceBools = bools.toBooleanArray()

            builder.apply {
                setTitle(getString(R.string.shareDialogTitleText))
                setMultiChoiceItems(urlTextList.toTypedArray(), null, { dialog, which, isChecked ->
                    multiChoiceBools[which] = !multiChoiceBools[which]
                })
                setPositiveButton(getString(R.string.share), { dialog, id ->
                    var checkbool = false
                    //共有する文字列
                    var shareText = shareHeader + "\n"
                    for (i in 0..urlList.size - 1) {
                        if (multiChoiceBools[i]){
                            shareText = shareText + "\n${urlTypeList[i]} :\n${urlList[i]}"
                            if (!checkbool) {checkbool = true}
                        }
                    }
                    val share: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    if (checkbool) {
                        startActivity(share)
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.shareDialogErrorMessage), Toast.LENGTH_LONG).show()
                    }
                })
                setNegativeButton(getString(R.string.cancel), { dialog, id ->
                    Toast.makeText(requireContext(),getString(R.string.shareDialogCancelMessage), Toast.LENGTH_SHORT)
                })
            }
            builder.create()
        }
        resultDialog.show()
    }
}