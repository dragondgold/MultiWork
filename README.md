## MultiWork Project

MultiWork is a project I have been working in my free time. It is an Android Based Measurement Tool. The smartphones have become very popular and almost everyone has one so I decided to start this project.

It is a Logic Analyzer, Frecuencimeter, LC Meter and Joystick to control my Robotic Arm (I will upload some photos later).
The hardware is made by me with a dsPIC Microcontroller and communicates with the smartphone through Bluetooth with an HC-06 module. 

You can see my article of this project in Spanish [here](http://tortimax.wordpress.com/2013/07/24/multiwork/).

## Logic Analyzer Features
* 8 channels available
* Capable of decoding UART and I2C communications
* Data can be showed in a list with the decoded data or in a chart to see communication waveform
* Smartphone and tablet adapted UI
* Up to 40MSPS
* Buffer size for 16000 samples
* Trigger by state changing for each channel
* More features and protocols will be added by the time...

## Requirements
* Android based device with at least Android Gingerbread 2.3</li>
* Electronic hardware which sample the data, schematics and PCB can be found [here](https://www.dropbox.com/sh/oq76xrg0jv6cvfu/KZ4UXd6o5D/MultiWork%20Altium)
* dsPIC firmware which can be found [here](https://github.com/dragondgold/MultiWork_dsPIC))

## Libraries/API
* MultiWork uses [achartengine](https://code.google.com/p/achartengine/) to show data waveform, thanks to Dan for this great framework and helping me to add annotations capabilities.
* The Protocol Decoder API used to decode the incoming data which is in <code>com.protocolanalyzer.api.andres</code>. The API is based on the abstract class <code>Protocol</code>, to create a new protocol just extends the <code>Protocol</code> class and create the corresponding <code>decode()</code> method. Example:
```java
public class I2CProtocol extends Protocol{

	public I2CProtocol(long freq) {
		super(freq);
	}

	@Override
	public void decode(double startTime) {
		// code
	}
    }
```

The bits which are going to be decoded must be saved on `logicData` member of `Protocol` and the `Strings` with the decoded data must be saved on `mDecodedData` in the class `TimePosition` which define the start and finish time of the decoded event corresponding to the string calculating approximates time based on the sample rate frequency. The `Strings` can easily be added using the `addString()` method.

## License

The Multiwork project is released under [BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause)
