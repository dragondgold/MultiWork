package com.bluetoothutils.andres;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Set;

public class DeviceScanner{

    private static final boolean DEBUG = true;
    private final OnDeviceSelected onDeviceSelected;

    private final ArrayList<String> devicesList = new ArrayList<String>();

    private BluetoothAdapter bluetoothAdapter;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog alertDialog;

    private final String ScanString;
    private final String ScanningString;
    private final String SelectDeviceString;
    private final String NoDeviceString;

    ArrayAdapter<String> adapter;
    ListView listView;

    public DeviceScanner(final OnDeviceSelected onDeviceSelected, final Activity activity, String scanString, String scanningString,
                         String selectDeviceString, String noDeviceString){

        this.onDeviceSelected = onDeviceSelected;
        ScanString = scanString;
        ScanningString = scanningString;
        SelectDeviceString = selectDeviceString;
        NoDeviceString = noDeviceString;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Dispositivos emparejados
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // Agrego los dispositivos emparejados a la lista
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                devicesList.add("[Paired] - " + device.getName() + "\n" + device.getAddress());
            }
        }

        // Registro Broadcast para cuando un dispositivo es encontrado
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(mReceiver, filter);

        // Registro Broadcast para cuando terminé de escanear
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(mReceiver, filter);

        // Creo el diálogo
        dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle("Bluetooth");
        dialogBuilder.setPositiveButton(ScanString, null);
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                bluetoothAdapter.cancelDiscovery();
                onDeviceSelected.onDeviceSelected(null, null);
                dialogInterface.cancel();
            }
        });

        // ListView
        listView = new ListView(activity);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(DEBUG) Log.i("DeviceScanner", "Item " + i + " clicked: " + devicesList.get(i));
                String info = devicesList.get(i);

                // La dirección MAC son los últimos 17 caracteres
                String address = info.substring(info.length() - 17);

                // Nombre
                String name;
                if(info.contains("[Paired]")){
                    name = info.substring(11, info.length() - 18);
                }
                else{
                    name = info.substring(0, info.length() - 18);
                }

                alertDialog.dismiss();
                activity.unregisterReceiver(mReceiver);
                bluetoothAdapter.cancelDiscovery();

                onDeviceSelected.onDeviceSelected(address, name);
            }
        });

        adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, devicesList);
        listView.setAdapter(adapter);
        dialogBuilder.setView(listView);

        alertDialog = dialogBuilder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (DEBUG) Log.i("DeviceScanner", "Scanning!");

                        devicesList.clear();
                        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();

                        bluetoothAdapter.startDiscovery();
                        alertDialog.setTitle(ScanningString);
                    }
                });
            }
        });
        alertDialog.show();
    }

    // BroadcastReceiver que es llamado en los eventos de Bluetooth
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Nuevo dispositivo encontrado
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Obtengo el dispositivo Bluetooth que fue encontrado
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Si no está emparejado lo agrego, de otro modo ya lo agregé al comienzo
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    devicesList.add(device.getName() + "\n" + device.getAddress());

                    if(DEBUG) Log.i("DeviceScanner", "Found: " + device.getName());
                    adapter.notifyDataSetChanged();
                }

            // Se termino de escanear
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                alertDialog.setTitle(SelectDeviceString);
                if (devicesList.size() == 0) {
                    alertDialog.setTitle(NoDeviceString);
                }
            }
        }
    };
}
