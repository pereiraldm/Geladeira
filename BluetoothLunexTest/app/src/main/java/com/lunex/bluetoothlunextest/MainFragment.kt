package com.lunex.bluetoothlunextest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lunex.bluetoothlunextest.databinding.FragmentMainBinding
import com.lunex.bt_def.BluetoothConstants
import com.lunex.bt_def.bluetooth.BluetoothController
import kotlin.concurrent.*

class MainFragment : Fragment(), BluetoothController.Listener {
    private lateinit var bluetoothController: BluetoothController
    private lateinit var btAdapter: BluetoothAdapter
    private lateinit var binding: FragmentMainBinding
    private var currentSeekBarValue = 0
    private val TAG = "MyActivity"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBtAdapter()
        val pref = activity?.getSharedPreferences(
            BluetoothConstants.PREFERENCES, Context.MODE_PRIVATE)
        val mac = pref?.getString(BluetoothConstants.MAC, "")
        bluetoothController = BluetoothController(btAdapter)


        binding.apply {
            // inicialmente desabilita todos os botões, exceto o botão de conectar
            setButtonsEnabled(false)
            btList.setOnClickListener {
                findNavController().navigate(R.id.listFragment)
            }
            connect.setOnClickListener {
                bluetoothController.connect(mac ?: "", this@MainFragment)
            }

            LigDes.setOnCheckedChangeListener { buttonView, isChecked ->
                if(isChecked) {
                    Toast.makeText(context, "Ligado", Toast.LENGTH_SHORT).show()
                    LigDes.isEnabled = false
                    LigDes.postDelayed({
                        LigDes.isEnabled = true
                        quente.isEnabled = true
                        quente.isClickable = true
                        neutro.isEnabled = true
                        neutro.isClickable = true
                        frio.isEnabled = true
                        frio.isClickable = true
                        avancado.isEnabled = true
                        avancado.isClickable = true
                    },1000)
                } else {
                    Toast.makeText(context, "Desligado", Toast.LENGTH_SHORT).show()
                    desligar()
                    quente.isEnabled = false
                    quente.isClickable = false
                    neutro.isEnabled = false
                    neutro.isClickable = false
                    frio.isEnabled = false
                    frio.isClickable = false
                    avancado.isChecked = false
                    avancado.isEnabled = false
                    avancado.isClickable = false
                    LigDes.isEnabled = false
                    LigDes.postDelayed({
                        LigDes.isEnabled = true
                    },1000)
                }
            }

            quente.setOnClickListener {
                ligarQuente()
                Toast.makeText(context, "Quente", Toast.LENGTH_SHORT).show()

                // Desabilitar os botões "Neutro" e "Frio" por 1 segundos
                neutro.isEnabled = false
                frio.isEnabled = false
                LigDes.isEnabled = false
                quente.postDelayed({
                    neutro.isEnabled = true
                    frio.isEnabled = true
                    LigDes.isEnabled = true
                }, 1000) // Atraso de 1 segundos em milissegundos
            }



            neutro.setOnClickListener {
                ligarNeutro()
                Toast.makeText(context, "Neutro", Toast.LENGTH_SHORT).show()

                // Desabilitar os botões "Neutro" e "Frio" por 1 segundos
                quente.isEnabled = false
                frio.isEnabled = false
                LigDes.isEnabled = false
                neutro.postDelayed({
                    quente.isEnabled = true
                    frio.isEnabled = true
                    LigDes.isEnabled = true
                }, 1000) // Atraso de 1 segundos em milissegundos
            }

            frio.setOnClickListener {
                ligarFrio()
                Toast.makeText(context, "Frio", Toast.LENGTH_SHORT).show()

                // Desabilitar os botões "Neutro" e "Frio" por 1 segundos
                neutro.isEnabled = false
                quente.isEnabled = false
                LigDes.isEnabled = false
                frio.postDelayed({
                    neutro.isEnabled = true
                    quente.isEnabled = true
                    LigDes.isEnabled = true
                }, 1000) // Atraso de 1 segundos em milissegundos
            }

            avancado.setOnCheckedChangeListener { buttonView, isChecked ->
                if(isChecked) {
                    Toast.makeText(context, "Avançado", Toast.LENGTH_SHORT).show()
                    quente.visibility = View.GONE
                    neutro.visibility = View.GONE
                    frio.visibility = View.GONE
                    LigDes.isEnabled = false
                    avancado.postDelayed({
                        seekBar.visibility = View.VISIBLE
                        LigDes.isEnabled = true
                    }, 1000) // Atraso de 1 segundos em milissegundos
                    ligarAvancado()
                    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            currentSeekBarValue = progress
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {

                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            bluetoothController.sendMessage("$currentSeekBarValue")
                        }

                    })
                } else {
                    desligarAvancado()
                    seekBar.visibility = View.GONE
                    LigDes.isEnabled = false
                    avancado.postDelayed({
                        quente.visibility = View.VISIBLE
                        neutro.visibility = View.VISIBLE
                        frio.visibility = View.VISIBLE
                        LigDes.isEnabled = true
                    }, 1000) // Atraso de 1 segundos em milissegundos
                }
            }
        }
    }

    // função para habilitar ou desabilitar todos os botões (exceto o botão de conectar)
    private fun setButtonsEnabled(enabled: Boolean) {
        binding.apply {
            LigDes.isEnabled = enabled
            quente.isEnabled = enabled
            neutro.isEnabled = enabled
            frio.isEnabled = enabled
            avancado.isEnabled = enabled
            seekBar.visibility = View.GONE
            avancado.isChecked = false
        }
    }


    private fun initBtAdapter(){
        val bManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bManager.adapter
    }

    @SuppressLint("SetTextI18n")
    override fun onReceive(message: String) {
        activity?.runOnUiThread {
            when(message){
                BluetoothController.BLUETOOTH_CONNECTED -> {
                    binding.connect.backgroundTintList = AppCompatResources
                        .getColorStateList(requireContext(), R.color.red)
                    binding.connect.text = "Desconectar"

                    // habilita todos os botões quando conectado
                    setButtonsEnabled(true)

                }
                BluetoothController.BLUETOOTH_NO_CONNECTED -> {
                    binding.connect.backgroundTintList = AppCompatResources
                        .getColorStateList(requireContext(), R.color.green)
                    binding.connect.text = "Conectar"


                    // desabilita todos os botões quando desconectado
                    setButtonsEnabled(false)
                }
                else -> {
                    Log.v(TAG, message)
                    binding.tvMessage.text = ("Temperatura: $message")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeConnection()
    }

    private fun ligarQuente(){
        bluetoothController.sendMessage("10")
    }

    private fun ligarNeutro(){
        bluetoothController.sendMessage("11")
    }

    private fun ligarFrio(){
        bluetoothController.sendMessage("01")
    }

    private fun desligar(){
        bluetoothController.sendMessage("00")
    }

    private fun ligarAvancado(){
        bluetoothController.sendMessage("avancado")
    }

    private fun desligarAvancado(){
        bluetoothController.sendMessage("desligarAvancado")
    }
}

private fun closeConnection() {
    TODO("Not yet implemented")
}