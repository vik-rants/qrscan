package com.example.qrscan


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.qrscan.databinding.ActivityMainBinding
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONException


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var qrScanIntegrator: IntentIntegrator? = null
    private var mCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setOnClickListener()
        setupScanner()
    }

    private fun setupScanner() {
        qrScanIntegrator = IntentIntegrator(this)
        qrScanIntegrator?.setOrientationLocked(false)
    }

    private fun setOnClickListener() {
        binding.btnScan.setOnClickListener { performAction() }

//        binding.showQRScanner.setOnClickListener {
//            // Add code to show QR Scanner Code in Fragment.
//            startActivity(Intent(this@MainActivity, HelperActivity::class.java))
//        }
    }

    private fun performWritetoSheet(registrationID: String) {
        Toast.makeText(this, "Writing to Sheet", Toast.LENGTH_LONG).show()
        var url="https://script.google.com/macros/s/AKfycbwpPow-1HSyeNHwAsBRgEJ5faf1Ro7ebwCmevdrVp1XYoNBywfXzhWVfxwDNkfz29ZQmg/exec?"
        url=url+"action=create&regID="+registrationID
        val requestQueue = Volley.newRequestQueue(this)
        val jsonRequest = JsonObjectRequest(Request.Method.GET,url, null,
            { response ->
                Log.d("Write Volley Success",response.toString())
            },
            { error ->
                // Handle errors
                Log.d("Volley Error","Error fetching API",error )
            })
        requestQueue.add(jsonRequest)
    }

    private fun performReadfromSheet( registrationID: String): String {
        Toast.makeText(this, "Reading from Sheet", Toast.LENGTH_LONG).show()
        var regCheckinStatus = ""
        var url="https://script.google.com/macros/s/AKfycbwpPow-1HSyeNHwAsBRgEJ5faf1Ro7ebwCmevdrVp1XYoNBywfXzhWVfxwDNkfz29ZQmg/exec?"
        url=url+"action=get&regID="+registrationID

        val requestQueue = Volley.newRequestQueue(this)
        val jsonRequest = JsonObjectRequest(Request.Method.GET,url, null,
            { response ->
                Log.d("Volley Success",response.toString())
                val regUserName=response.getString("user_name")
                regCheckinStatus=response.getString("chekin_status")
                binding.userName.text = regUserName

            },
            { error ->
                // Handle errors
                Log.d("Volley Error","Error fetching API",error )
            })
        requestQueue.add(jsonRequest)
        return regCheckinStatus
    }
    private fun performAction() {
        // Code to perform action when button is clicked.
        qrScanIntegrator?.initiateScan()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            // If QRCode has no data.
            if (result.contents == null) {
                Toast.makeText(this, getString(R.string.result_not_found), Toast.LENGTH_LONG).show()
            } else {
                // If QRCode contains data.
                try {
                    // Converting the data to json format
                    //val obj = JSONObject(result.contents)

                    // Show values in UI.
                    //binding.name.text = obj.getString("name")
                    //binding.siteName.text = obj.getString("site_name")
                    // Parse values and then show in UI.
                    val scannedData = result.contents.split("+").toTypedArray()
                    binding.regID.text = scannedData[0]
                    binding.peopleCount.text = scannedData[1]
                    // Increment the counter
                    mCounter += scannedData[1].toInt()
                    binding.countKey.text = mCounter.toString()

                    //post processing
                    val registationid=binding.regID.text.toString()
                    val checkStatus = performReadfromSheet(registationid)
                    //SendRequest().execute()
                    if (checkStatus == "NotCheckedIn") {
                        binding.checkinStatus.text = "NotCheckedIn"
                        Toast.makeText(this, "User not checked in", Toast.LENGTH_LONG).show()
                        performWritetoSheet(registationid)

                    } else if (checkStatus == "CheckedIn") {
                        binding.checkinStatus.text = checkStatus
                        Toast.makeText(this, "User is already checked in", Toast.LENGTH_LONG).show()

                    }


                } catch (e: JSONException) {
                    e.printStackTrace()

                    // Data not in the expected format. So, whole object as toast message.
                    Toast.makeText(this, result.contents, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    //Custom Changes
/*    class SendRequest : AsyncTask<String?, Void?, String?>() {
        override fun onPreExecute() {}
        override fun doInBackground(vararg arg0: String?): String {
            return try {
                val url =
                    URL("https://script.google.com/macros/s/AKfycbzI9hP7unJMQGhufRToTW3kNBPYMMwYdQ-9hGvTntRWIFjjWCYCjSzJA2dHp9yRXdjf/exec") //Paste here your AppScript URL that is generated after deploying it as a Web App
                val postDataParams = JSONObject()
                postDataParams.put("sdata", scannedData) //Variable same as declared in AppScript
                Log.e("params", postDataParams.toString())
                val conn = url.openConnection() as HttpURLConnection
                conn.readTimeout = 15000
                conn.connectTimeout = 15000
                conn.requestMethod = "GET"
                conn.doInput = true
                conn.doOutput = true
                val os = conn.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(getPostDataString(postDataParams))
                writer.flush()
                writer.close()
                os.close()
                val responseCode = conn.responseCode
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    val `in` = BufferedReader(InputStreamReader(conn.inputStream))
                    val sb = StringBuffer("")
                    var line: String?
                    while (`in`.readLine().also { line = it } != null) {
                        sb.append(line)
                        break
                    }
                    `in`.close()
                    sb.toString()
                } else {
                    String("false : $responseCode")
                }
            } catch (e: Exception) {
                String("Exception: " + e.message)
            }
        }

        override fun onPostExecute(result: String?) {
            Toast.makeText(this, result, Toast.LENGTH_LONG).show()
        }
    }

    @Throws(Exception::class)
    fun getPostDataString(params: JSONObject): String? {
        val result = StringBuilder()
        var first = true
        val itr = params.keys()
        while (itr.hasNext()) {
            val key = itr.next()
            val value = params[key]
            if (first) first = false else result.append("&")
            result.append(URLEncoder.encode(key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(value.toString(), "UTF-8"))
        }
        return result.toString()
    }*/
}