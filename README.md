# WifiPickerTester
Launches WifiPickerActivity and shows the resultCode and Intent

This exposes a bug in Android 11: Launching WifiPickerActivity will never give you a resultCode of
`RESULT_OK`.  This is the result of launching the Activity (at time 00:30:37) and pressing the Next
button (at time 00:30:42), with `DEBUG_RESULTS` and `DEBUG_ACTIVITY_STARTS` set to true inside of
`frameworks/base/services/core/java/com/android/server/wm/ActivityTaskManagerDebugConfig.java`:

```
$ adb logcat *:S SettingsActivity ActivityTaskManager SettingsPreference
09-25 00:30:37.214  1033  2237 I ActivityTaskManager: START u0 {act=android.net.wifi.PICK_WIFI_NETWORK cmp=com.android.settings/.wifi.WifiPickerActivity (has extras)} from uid 10136
09-25 00:30:37.214  1033  2237 V ActivityTaskManager: Will send result to Token{25a6b8a ActivityRecord{40005fb u0 com.example.wifipickertester/.MainActivity t88}} ActivityRecord{40005fb u0 com.example.wifipickertester/.MainActivity t88}
09-25 00:30:37.215  1033  2237 D ActivityTaskManager: Activity start allowed: callingUidHasAnyVisibleWindow = 10136, isCallingUidPersistentSystemProcess = false
09-25 00:30:37.231  1033  2237 W ActivityTaskManager: Tried to set launchTime (0) < mLastActivityLaunchTime (824970)
09-25 00:30:37.244  1970  1970 D SettingsActivity: Starting onCreate
09-25 00:30:37.249  1970  1970 D SettingsActivity: Starting to set activity title
09-25 00:30:37.249  1970  1970 D SettingsActivity: Done setting title
09-25 00:30:37.249  1970  1970 D SettingsActivity: Switching to fragment com.android.settings.wifi.WifiSettings
09-25 00:30:37.252  1033  1046 I ActivityTaskManager: START u0 {act=android.settings.WIFI_SETTINGS2 cmp=com.android.settings/.Settings$WifiSettings2Activity (has extras)} from uid 1000
09-25 00:30:37.253  1033  1046 V ActivityTaskManager: Will send result to Token{c727a59 ActivityRecord{7db661e u0 com.android.settings/.wifi.WifiPickerActivity t88}} ActivityRecord{7db661e u0 com.android.settings/.wifi.WifiPickerActivity t88}
09-25 00:30:37.253  1033  1046 D ActivityTaskManager: Activity start allowed for important callingUid (1000)
09-25 00:30:37.260  1033  1046 V ActivityTaskManager: Finishing activity r=ActivityRecord{7db661e u0 com.android.settings/.wifi.WifiPickerActivity t88}, result=0, data=null, reason=app-request
09-25 00:30:37.260  1033  1046 V ActivityTaskManager: Adding result to ActivityRecord{40005fb u0 com.example.wifipickertester/.MainActivity t88} who=null req=0 res=0 data=null
09-25 00:30:37.260  1970  1970 D SettingsActivity: Executed frag manager pendingTransactions
09-25 00:30:37.263  1033  1046 W ActivityTaskManager: Tried to set launchTime (0) < mLastActivityLaunchTime (862021)
09-25 00:30:37.275  1970  1970 D SettingsActivity: Starting onCreate
09-25 00:30:37.280  1970  1970 D SettingsActivity: Starting to set activity title
09-25 00:30:37.280  1970  1970 D SettingsActivity: Done setting title
09-25 00:30:37.280  1970  1970 D SettingsActivity: Switching to fragment com.android.settings.wifi.WifiSettings2
09-25 00:30:37.301  1970  1970 D SettingsActivity: Executed frag manager pendingTransactions
09-25 00:30:37.325  1970  4703 D SettingsActivity: No enabled state changed, skipping updateCategory call
09-25 00:30:37.367  1033  1057 I ActivityTaskManager: Displayed com.android.settings/.Settings$WifiSettings2Activity: +146ms
09-25 00:30:37.393  1033  1046 V ActivityTaskManager: No result destination from ActivityRecord{7db661e u0 com.android.settings/.wifi.WifiPickerActivity t88 f}}
09-25 00:30:42.131  1033  1635 V ActivityTaskManager: Finishing activity r=ActivityRecord{6481b91 u0 com.android.settings/.Settings$WifiSettings2Activity t88}, result=-1, data=null, reason=app-request
09-25 00:30:42.132  1033  1635 V ActivityTaskManager: No result destination from ActivityRecord{6481b91 u0 com.android.settings/.Settings$WifiSettings2Activity t88 f}}
09-25 00:30:42.158  1033  1046 V ActivityTaskManager: Delivering results to ActivityRecord{40005fb u0 com.example.wifipickertester/.MainActivity t88}: [ResultInfo{who=null, request=0, result=0, data=null}]
09-25 00:30:42.676  1033  1046 V ActivityTaskManager: No result destination from ActivityRecord{6481b91 u0 com.android.settings/.Settings$WifiSettings2Activity t88 f}}
```

We can see that 
1. `WifiPickerActivity` is started, and it will send results to our MainActivity in this app.
2. `WifiSettings2Activity` is started (because the `WifiSettings` Fragment launches
   `WifiSettings2Activity` from the FeatureFlag if statement), and it will send results to
    WifiPickerActivity. `WifiPickerActivity` then finishes. This is done by the [WifiSettings
    Fragment](https://android.googlesource.com/platform/packages/apps/Settings/+/b33a59b860077e6e0c07b7b232adf4af156645ee/src/com/android/settings/wifi/WifiSettings.java#241):
 
    ```
    if (FeatureFlagUtils.isEnabled(getContext(), FeatureFlagUtils.SETTINGS_WIFITRACKER2)) {
       final Intent intent = new Intent("android.settings.WIFI_SETTINGS2");
       final Bundle extras = getActivity().getIntent().getExtras();
       if (extras != null) {
           intent.putExtras(extras);
       }
       getContext().startActivity(intent);
       finish();
       return;
    }
   ```
   
3. When `WifiPickerActivity` finishes, our MainActivity will get a resultCode of 0
   (`RESULT_CANCELLED`)
4. When `WifiSettings2Activity` finishes, `WifiPickerActivity` already finished, so there is "no
   result destination".

The `WifiSettings` Fragment is [going to be removed in the
future](https://android.googlesource.com/platform/packages/apps/Settings/+/b33a59b860077e6e0c07b7b232adf4af156645ee%5E%21/#F6),
so workarounds should be reverted when it's done.
