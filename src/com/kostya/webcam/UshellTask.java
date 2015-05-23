package com.kostya.webcam;

import android.content.Context;
import android.content.Intent;
import org.apache.http.util.ByteArrayBuffer;

/**
 * Created by Kostya on 28.08.14.
 */
public class UshellTask {

    final Context context;
    final BluetoothServer camServer;
    ByteArrayBuffer command = new ByteArrayBuffer(0);
    enumCommand commandType = enumCommand.CMD_NONE;
    boolean parameterFlag = false;

    private final int CR = 0x0D;
    private final int LF = 0x0A;
    private final String CR_LF = "\r\n";
    private final int CTRL_C = 0x03;
    private final int BACKSPACE_CHAR = 0x08;
    private final int ABORT_CHAR = CTRL_C;

    static final String STR_VRS = "VRS";    //Версия весов
    static final String STR_TPH = "TPH";    //Сделать фото


    UshellTask(Context context, BluetoothServer s) {
        this.context = context;
        camServer = s;
    }

    public static enum enumCommand {
        CMD_NONE,
        //======================================================
        CMD_VRS,    //версия веб камеры
        CMD_TPH,    //Сделать фото
        CMD_BTH,    //Bluetooth
        CMD_GPF,    //Получить настройки
        CMD_ERR     //Ошибки
    }

    synchronized void buildCommand(byte b) {

        switch (b) {
            case CR:
                //command.append((byte)0);//Add NULL char
                break;
            case ABORT_CHAR:    //^c abort cmd
                command.clear();
                break;
            case LF:
                String str = new String(command.toByteArray());
                parseCommand(str);
                command.clear();
                break;
            default:
                command.append(b);
                break;
        }
    }

    void parseCommand(String cmd) {
        //cmd = true;
        //Decode command type
        if (cmd.contains(STR_VRS))      //версия весов
            commandType = enumCommand.CMD_VRS;
        else if (cmd.contains(STR_TPH))    //Сделать фото
            commandType = enumCommand.CMD_TPH;
        else {
            //cmd = false;
            return;
        }

        //Get first arg (if any)
        String parameter = "";
        if (cmd.length() > 3) {
            parameterFlag = true;
            parameter = cmd.substring(3, cmd.length());
        } else
            parameterFlag = false;

        ushellTask(commandType, parameter);
    }

    synchronized void ushellTask(enumCommand type, String parameter) {

        switch (type) {
            case CMD_VRS:
                camServer.send(STR_VRS);
                camServer.send(BluetoothServer.CAM_VERSION);
                //scale.send(CR_LF);
                break;
            case CMD_TPH:
                context.startService(new Intent(context.getApplicationContext(), TakeService.class).setAction("take").putExtra(context.getString(R.string.key_flag_take_single), true));
                camServer.send(STR_TPH);
                break;
            default: {
            }

            camServer.send(CR_LF);

            parameterFlag = false;
        }

    }

}
