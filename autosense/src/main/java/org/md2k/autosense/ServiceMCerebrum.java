package org.md2k.autosense;

import android.content.Intent;
import android.os.Bundle;

import org.md2k.autosense.antradio.connection.ServiceAutoSenses;
import org.md2k.mcerebrum.commons.app_info.AppInfo;
import org.md2k.mcerebrum.core.access.AbstractServiceMCerebrum;

public class ServiceMCerebrum extends AbstractServiceMCerebrum {
    public ServiceMCerebrum() {
    }


    @Override
    protected boolean hasClear() {
        return false;
    }

    @Override
    public void initialize(Bundle bundle) {
        Intent intent=new Intent(this, ActivityMain.class);
        intent.putExtra("PERMISSION",true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void launch(Bundle bundle) {
        Intent intent=new Intent(this, ActivityMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void startBackground(Bundle bundle) {
        if(!isRunning() && isConfigured())
            startService(new Intent(this, ServiceAutoSenses.class));
    }

    @Override
    public void stopBackground(Bundle bundle) {
        if(isRunning())
        stopService(new Intent(this, ServiceAutoSenses.class));
    }

    @Override
    public void report(Bundle bundle) {
/*
        Intent intent = new Intent(this, ActivityPlotChoice.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
*/
    }

    @Override
    public void clear(Bundle bundle) {

    }

    @Override
    public boolean hasReport() {
        return false;
    }

    @Override
    public boolean isRunInBackground() {
        return true;
    }

    @Override
    public long getRunningTime() {
        return AppInfo.serviceRunningTime(this, ServiceAutoSenses.class.getName());
    }

    @Override
    public boolean isRunning() {
        return AppInfo.isServiceRunning(this, ServiceAutoSenses.class.getName());
    }

    @Override
    public boolean isConfigured() {
        return Configuration.isConfigured();
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean hasInitialize() {
        return true;
    }

    @Override
    public void configure(Bundle bundle) {
        Intent intent = new Intent(this, ActivitySettings.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public boolean isEqualDefault() {
        return Configuration.isEqualDefault();
    }

}
