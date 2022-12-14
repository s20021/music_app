package jp.ac.it_college.std.s20021.myapplication

import  android.app.Application
import android.content.Context
import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity

class DataClass() {
    private val root_url: String = "https://musicbrainz.org/ws/2/"
    private val fmt: String = "&fmt=json" //フォーマットをjson指定

    private val displayed_result_default: Int = 25 //表示件数

    fun get_displayed_default(): Int {
        return displayed_result_default
    }

    //エンティティのリスト
    private val entity_map = mapOf<String, String>(
        "アーティスト" to "artist",
        "リリース" to "release",
        "リリースグループ" to "release-group",
        "レコーディング" to "recording",
        "イベント" to "event",
        "ジャンル" to "genre",
        "レーベル" to "label",
        "エリア" to "area",
        "楽器" to "instrument"
    )

    //エンティティリストの取得
    fun get_entitys(): Map<String, String> {
        return entity_map
    }

    //APIのURLの取得
    fun get_url_search(entity: String, query: String, limit: Int, offset: Int): String {
        /**
        if (entity == "genre") {
            return "${root_url}${entity}/all?limit=${limit}&offset=${offset}$fmt"
        }
        */
        return "${root_url}${entity}?query=${query}&limit=${limit}&offset=${offset}$fmt"
    }

    fun get_url_lookup(entity: String, MBID: String, inc: String = "aliases"): String {
        return "${root_url}${entity}/${MBID}?inc=${inc}$fmt"
    }

    fun get_url_urlels(entity: String, MBID: String, inc: String = "url-rels"): String {
        return "${root_url}${entity}/${MBID}?inc=${inc}$fmt"
    }

    fun get_url_RelRels(entity: String, MBID: String, inc: String = "releases+url-rels"): String {
        return "${root_url}${entity}/${MBID}?inc=${inc}$fmt"
    }
}