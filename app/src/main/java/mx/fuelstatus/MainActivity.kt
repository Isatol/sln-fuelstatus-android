package mx.fuelstatus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import mx.fuelstatus.ui.FuelApp

class MainActivity : ComponentActivity() {
    private val model: FuelViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FuelApp(model) }
    }
    override fun onStart() { super.onStart(); model.onForeground() }
}
