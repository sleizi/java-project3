package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.imageservice.FakeImageService;
import com.udacity.catpoint.imageservice.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private final ImageService imageService = new FakeImageService();

    @InjectMocks
    SecurityService securityService;

    Sensor sensor;

    @Mock
    StatusListener statusListener;

    @BeforeEach
    void setUp() {
        sensor = new Sensor("New Sensor", SensorType.MOTION);
    }

    @Test
    @DisplayName("1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.")
    void setAlarmStatus__alarmArmed_sensorActivated__thenPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    @DisplayName("2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm on.")
    void setAlarmStatus__alarmArmed_sensorActivated_alarmPending__thenOn() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("3. If pending alarm and all sensors are inactive, return to no alarm state.")
    void setAlarmStatus__sensorInactivated_alarmPending__thenNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @DisplayName("4. If alarm is active, change in sensor state should not affect the alarm state.")
    @ParameterizedTest(name = "Change sensor activation status to be {0}")
    @ValueSource(booleans = {true, false})
    void changeSensorState__alarmActivated__thenAlarmStateNotChanged(boolean activationStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(!activationStatus);
        securityService.changeSensorActivationStatus(sensor, activationStatus);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    @DisplayName("5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.")
    void activeSensor__sensorActivated_alarmPending__thenAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("6. If a sensor is deactivated while already inactive, make no changes to the alarm state.")
    void deactivateSensor__sensorDeactivated__alarmPending__thenAlarmStateNotChanged() {
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    @DisplayName("7. If the camera image contains a cat while the system is armed-home, put the system into alarm status.")
    void setAlarmStatus__imageCat_armedHome__thenAlarm() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        BufferedImage image = new BufferedImage(1, 1, 1);
        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(true);
        securityService.processImage(image);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("8. If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.")
    void setAlarmStatus__imageNotCat_sensorNotActivated__thenNoAlarm() {
        sensor.setActive(false);
        BufferedImage image = new BufferedImage(1, 1, 1);
        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(false);
        securityService.processImage(image);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("9. If the system is disarmed, set the status to no alarm.")
    void setAlarmStatus__disArmed__thenNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @DisplayName("10. If the system is armed, reset all sensors to inactive.")
    @ParameterizedTest(name = "Change Arming status to {0}")
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void setArmingStatus__Armed__thenSensorsDeactivated(ArmingStatus armingStatus) {
        sensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(armingStatus);
        assertEquals(sensor.getActive(), false);
    }


    @Test
    @DisplayName("11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.")
    void setArmingStatus__armedHome_imageCat__thenAlarm() {
        BufferedImage image = new BufferedImage(1, 1, 1);
        when(imageService.imageContainsCat(image, 50.0f)).thenReturn(true);
        securityService.processImage(image);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void getAlarmStatus__thenSuccess() {
        securityService.getAlarmStatus();
        verify(securityRepository).getAlarmStatus();
    }

    @Test
    void addSensor__thenSuccess() {
        securityService.addSensor(sensor);
        verify(securityRepository).addSensor(sensor);
    }

    @Test
    void removeSensor__thenSuccess() {
        securityService.removeSensor(sensor);
        verify(securityRepository).removeSensor(sensor);
    }

    @Test
    void addAndRemoveStatusListener__thenSuccess() {
        securityService.addStatusListener(statusListener);
        Set<StatusListener> statusListeners = securityService.getStatusListeners();
        assertEquals(statusListeners.size(), 1);
        securityService.removeStatusListener(statusListener);
        statusListeners = securityService.getStatusListeners();
        assertEquals(statusListeners.size(), 0);
    }

    @Test
    void changeSensorActivationStatus__disArmed__thenDoNothing() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).getAlarmStatus();
    }
}