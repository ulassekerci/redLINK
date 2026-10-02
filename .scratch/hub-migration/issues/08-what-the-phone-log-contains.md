# What the phone log contains

Type: grilling
Status: resolved

Map: [Hub migration](../map.md)

## Question

The phone log is the log of record. What is in it and how is it organised?

Decide: columns and units (raw board values, derived values such as speed and power, GPS); sample rate; what begins and ends a log file and how that relates to a trip; behaviour when BLE drops mid-file; retention on the phone; how files are named and exported through the share sheet.

## Answer

The log of record is one plain CSV per run, one row per poll cycle, with speed and distance taken from the board rather than computed by us. Decided by grilling on 2026-10-02. Nothing here was run on a phone.

- **Polling:** the phone polls `COMM_GET_VALUES_SETUP` (47) and `COMM_GET_DECODED_ADC` (32) at 20 Hz. `COMM_GET_VALUES` (4) is no longer polled. The setup command returns speed in m/s and distance in metres computed from the board's own wheel diameter, gear ratio and pole count, plus battery level, odometer and board uptime. Lost with the old command: d and q axis currents and voltages, the three separate MOSFET temperatures, raw tachometer counts and the timeout flag. Polling both was rejected: three commands per 50 ms cycle and about 70 more bytes per cycle to every viewer, for fields nobody reads.
- **Board settings are the only source of speed:** the hard-coded 0.5 m wheel, 5.88 gear and 22 poles leave both apps. A VESC Tool log from 2026-09-26 shows the board reporting 465.5 m for 107,426 tachometer counts, where our constants give 434.8 m, so the two disagree by about 7% and it is unknown which is right. Checking the board's settings against the car goes to [Board speed settings and firmware version](14-board-speed-settings-and-firmware-version.md).
- **Layout:** comma-separated, decimal points, the column names as the first line and no comment lines. The file is read mostly by giving it to Claude and asking for graphs, so names carry their unit. Compatibility with VESC Tool's log analysis was considered and dropped: its format is read by column position, has no ADC columns and only a time of day.
- **Columns:** `time_utc, elapsed_s, speed_m_s, distance_m, distance_abs_m, erpm, duty_cycle, battery_voltage_v, battery_current_a, motor_current_a, power_w, battery_level, energy_used_wh, energy_charged_wh, charge_used_ah, charge_charged_ah, mosfet_temp_c, motor_temp_c, fault_code, adc_level1, adc_level2, odometer_m, board_uptime_ms, gps_fix_time_utc, gps_lat_deg, gps_lon_deg, gps_alt_m, gps_speed_m_s, gps_heading_deg, gps_accuracy_m`. `power_w` (voltage times battery current) is the only value the phone computes.
- **Time:** `time_utc` is the phone's wall clock in ISO 8601 with milliseconds. `elapsed_s` is seconds since the run started with three decimals (millisecond resolution, e.g. `12.350`), from the monotonic clock, so intervals stay true when Android corrects the wall clock.
- **Rows:** one per poll cycle in which at least one reply arrived; a missing reply leaves its cells empty. The latest GPS fix is repeated on every row, and `gps_fix_time_utc` shows when it was taken. While the board is unreachable there is one row per GPS fix (1 Hz) with the board cells empty. A flag column for this was dropped as redundant.
- **Files:** one file per run. A run resumed after Android kills the service appends to the same file; the gap shows in the time columns. The file is flushed to storage once a second, so a kill or a dead battery loses at most about a second.
- **Name and place:** `redLINK_<local start time>.csv`, for example `redLINK_2026-09-26_14-22-09.csv`, in the phone's shared `Documents/redLINK/` folder. That needs no storage permission on Android 10 and later, and the files are reachable from the Files app and over USB even if the app is broken or uninstalled (from the Android documentation). After a reinstall the app no longer owns earlier files, so they drop out of its run list while staying in the folder; picking the folder through the system picker was rejected as one more setup step.
- **Retention:** nothing is deleted automatically. The app lists runs with their total size and a person deletes them. About 15 MB per hour of running.
- **Export:** share one run, or several selected runs, through the share sheet. Sharing a run still in progress is not needed.
- **Trips:** the log knows nothing about trips. A file is a run; a trip stays a viewer-side measurement.

Handed on: the change of polled command amends [What travels on the stream](05-what-travels-on-the-stream.md); the viewer's parser, the direct-Bluetooth polling and the trip meter's inputs go to [Desktop app architecture](11-desktop-app-architecture.md); the run list with share and delete goes to [What the phone screen shows](09-what-the-phone-screen-shows.md).
