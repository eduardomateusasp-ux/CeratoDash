package br.com.ceratodash

import android.app.Application
import br.com.ceratodash.ui.Theme

class CeratoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Theme.init(this)
    }
}
