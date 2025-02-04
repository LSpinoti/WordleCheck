package com.lukahax.wordlecheck

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.beust.klaxon.Klaxon
import java.io.File
import java.io.PrintWriter
import java.net.InetAddress
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    // Fields
    var json: WordBank?
    var word: String?

    // Set defaults
    init {
        json = null
        word = "CIGAR"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

	// Download 
        val downthread = DownloadThread(filesDir, this@MainActivity)
        if (connected()) {
            downthread.start()
        }
        else {
            Toast.makeText(applicationContext, "Cannot connect to internet", Toast.LENGTH_SHORT)
        }

        try {
	    // Parse json
            this.json =
                Klaxon().parse<WordBank>(File(filesDir, "words.json").readText(Charsets.UTF_8))

	    // Make wordBox all-caps
            val wordBox = findViewById<EditText>(R.id.wordBox)
            val editfilters = wordBox.filters + InputFilter.AllCaps()
            wordBox.filters = editfilters
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun check(view: View) {
        val date = LocalDate.now()

        val wordBox = findViewById<EditText>(R.id.wordBox)
        word = wordBox.text.toString().lowercase()

	// Make sure length is 5
        if (word!!.length < 5) {
            val output = findViewById<TextView>(R.id.textView)
            val subview = findViewById<TextView>(R.id.subView)

            output.text = "<5 LETTERS"
            subview.text = ""

            return
        }

	// Check if word is used
        if (json != null) {
            if (json!!.contains(word!!)) {
                if (date.isAfter(json!!.wordToDate(word!!))) {
                    used()
                }
                else {
                    valid()
                }
            }
            else {
                valid()
            }
        }
    }

    // Run when a used word is found
    @RequiresApi(Build.VERSION_CODES.O)
    fun used() {
	// Set appearance
        val output = findViewById<TextView>(R.id.textView)
        val subview = findViewById<TextView>(R.id.subView)
        val bg = findViewById<ConstraintLayout>(R.id.frame)
        val button = findViewById<Button>(R.id.button)
        val update = findViewById<Button>(R.id.update)
        val wordBox = findViewById<EditText>(R.id.wordBox)
        bg.setBackgroundColor(getColor(R.color.red))
        button.setBackgroundColor(getColor(R.color.white))
        button.setTextColor(getColor(R.color.red))
        wordBox.setTextColor(getColor(R.color.white))
        wordBox.backgroundTintList = getColorStateList(R.color.white)
        output.setTextColor(getColor(R.color.white))
        update.visibility = View.INVISIBLE
        output.text = "USED"

	// Display the date the word was used
        subview.text = "on " + json!!.wordToDate(word!!).format(DateTimeFormatter.ofPattern("MMMM d, uuuu"))
    }

    // Run if the word is not used
    @RequiresApi(Build.VERSION_CODES.O)
    fun valid() {
	// Set appearance
        val output = findViewById<TextView>(R.id.textView)
        val subview = findViewById<TextView>(R.id.subView)
        val bg = findViewById<ConstraintLayout>(R.id.frame)
        val button = findViewById<Button>(R.id.button)
        val update = findViewById<Button>(R.id.update)
        val wordBox = findViewById<EditText>(R.id.wordBox)
        bg.setBackgroundColor(getColor(R.color.green))
        button.setBackgroundColor(getColor(R.color.white))
        button.setTextColor(getColor(R.color.green))
        wordBox.setTextColor(getColor(R.color.white))
        wordBox.backgroundTintList = getColorStateList(R.color.white)
        output.setTextColor(getColor(R.color.white))
        update.setTextColor(getColor(R.color.green))
        update.setBackgroundColor(getColor(R.color.white))
        output.text = "NOT USED"

	// Use the latest date available in the JSON to say "as of"
        if (json!!.latestDate() < LocalDate.now().minusDays(1)) {
            subview.text = "as of " + json!!.latestDate().format(DateTimeFormatter.ofPattern("MMMM d, uuuu"))
            update.visibility = View.VISIBLE
        }
        else {
            subview.text = "as of " + LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("MMMM d, uuuu"))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun update() {
        if (connected()) {
            val downthread = DownloadThread(filesDir, this@MainActivity)
            downthread.start()
            try {
		// Parse json
                this.json =
                    Klaxon().parse<WordBank>(File(filesDir, "words.json").readText(Charsets.UTF_8))

		// Make wordbox all-caps
                val wordBox = findViewById<EditText>(R.id.wordBox)
                val editfilters = wordBox.filters + InputFilter.AllCaps()
                wordBox.filters = editfilters
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
        else {
            Toast.makeText(applicationContext, "Cannot connect to internet", Toast.LENGTH_SHORT)
        }
    }

    // Check internet connectivity
    @RequiresApi(Build.VERSION_CODES.O)
    fun connected(): Boolean {
        return try {
            val ipAddr = InetAddress.getByName("google.com")
            !ipAddr.equals("")
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

class DownloadThread(filesDir: File, con: Context): Thread() {
    private val fd: File
    private val cont: Context

    init {
        fd = filesDir
        cont = con
    }

    // Downloads words.json
    @RequiresApi(Build.VERSION_CODES.O)
    public override fun run() {
        try {
	    // The gist at the following URL gets updated every day at 12:00
	    // Note that NYT keeps the following day's word available
            val url = URL("https://gist.githubusercontent.com/LSpinoti/7590cb7e52132b01356dbca98d7d44e1/raw/c69cf956a63350b0c6b85d84692986eb9092bd05/words.json")
            val connection = url.openConnection()
            val inputStream = connection.getInputStream()
            val text = inputStream.bufferedReader().use {it.readText()}
            val wordfile = File(fd, "words.json")
            val pw = PrintWriter(wordfile)
            pw.close()
            wordfile.writeText(text)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class WordBank(val data: Array<Word>) {

    // Checks if word exists
    fun contains(word: String): Boolean {
        for (e in data) {
            if (e.word == word)
                return true
        }

        return false
    }

    // Retrieves the date the given word was used
    @RequiresApi(Build.VERSION_CODES.O)
    fun wordToDate(word: String): LocalDate {
        for (e in data) {
            if (e.word == word)
                return LocalDate.parse(e.date)
        }

        return LocalDate.parse("0001-01-01")
    }

    // Returns the date of the newest word
    @RequiresApi(Build.VERSION_CODES.O)
    fun latestDate(): LocalDate {
        return LocalDate.parse(data.last().date)
    }
}

data class Word(val date: String, val word: String)
