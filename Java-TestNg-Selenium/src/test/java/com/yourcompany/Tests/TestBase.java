package com.yourcompany.Tests;

// import Sauce TestNG helper libraries

import com.yourcompany.Pages.MobileNative.Android.MobileNativeAndroidGuineaPigPage;
import com.yourcompany.Pages.PageFactories.MobileNative.AndroidNativePageFactory;
import com.yourcompany.Pages.PageFactories.MobileNative.IosNativePageFactory;
import com.yourcompany.Pages.PageFactories.MobileNativePageFactory;
import com.yourcompany.Pages.PageFactories.MobileWebPageFactory;
import com.yourcompany.Pages.PageFactories.DesktopWebPageFactory;
import com.yourcompany.Pages.PageFactories.PageFactory;
import com.yourcompany.Utils.JsonMapperUtil;
import com.yourcompany.Utils.TOUtils;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.ITestResult;
import com.yourcompany.Utils.SauceUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.UnexpectedException;

// import testng annotations
// import java libraries

/**
 * Simple TestNG test which demonstrates being instantiated via a DataProvider in order to supply multiple browser combinations.
 *
 * @author Neil Manvar
 *
 * Edited by Marina on 6/27/17 to allow for the use of lists of devices and browsers parsed from json files
 */
public class TestBase  {

    protected PageFactory pageFactory = null;
    protected MobileNativePageFactory mobileNativePageFactory = null;

    /**
     * ThreadLocal variable which contains the  {@link WebDriver} instance which is used to perform browser interactions with.
     */
    private ThreadLocal<WebDriver> webDriver = new ThreadLocal<WebDriver>();

    /**
     * ThreadLocal variable which contains the Sauce Job Id.
     */
    private ThreadLocal<String> sessionId = new ThreadLocal<String>();

    /**
     * DataProvider that explicitly sets the browser combinations to be used.
     *
     * @param testMethod
     * @return Two dimensional array of objects with browser, version, and platform information
     */
    @DataProvider(name = "browsers", parallel = true)
    public static Object[][] sauceBrowserDataProvider(Method testMethod) {
        return JsonMapperUtil.getObjectArraysFromJsonFile("src/test/resources/browsersCfg.json");
    }

    @DataProvider(name = "devices", parallel = true)
    public static Object[][] sauceDevicesDataProvider(Method testMethod) {
        return JsonMapperUtil.getObjectArraysFromJsonFile("src/test/resources/devicesCfg.json");
    }

    /**
     * @return the {@link WebDriver} for the current thread
     */
    public WebDriver getWebDriver() {
        return webDriver.get();
    }


    /**
     *
     * @return the Sauce Job id for the current thread
     */
    public String getSessionId() {
        return sessionId.get();
    }


    /**
     * Constructs a new {@link RemoteWebDriver} instance which is configured to use the capabilities defined by the browser,
     * version and os parameters, and which is configured to run against ondemand.saucelabs.com, using
     * the username and access key populated by the {@link #authentication} instance.
     *
     * @param browser Represents the browser to be used as part of the test run.
     * @param version Represents the version of the browser to be used as part of the test run.
     * @param os Represents the operating system to be used as part of the test run.
     * @param methodName Represents the name of the test case that will be used to identify the test on Sauce.
     * @return
     * @throws MalformedURLException if an error occurs parsing the url
     */
    protected void createDriver(String browser, String version, String os, String pageobject, String methodName)
            throws MalformedURLException, UnexpectedException {

        DesiredCapabilities capabilities = null;
        String url = "";

        if (TOUtils.isTO(browser)) {
            capabilities = TOUtils.CreateCapabilities(browser, version, os, pageobject, methodName);
            url = TOUtils.getURL();
        } else { // Test Object
            capabilities = SauceUtils.CreateCapabilities(browser, version, os, pageobject, methodName);
            url = SauceUtils.getURL();
        }



        if (pageobject.contains("MobileNative")) {
            // Launch remote browser and set it as the current thread
            if (pageobject.contains("Android")) {
                // TO DO: set desired caps for app (sim and emu on Sauce)
                webDriver.set(new AndroidDriver(
                        new URL(url),
                        capabilities));
            } else {
                webDriver.set(new IOSDriver(
                        new URL(url),
                        capabilities));
            }

            // set current sessionId
            sessionId.set(((RemoteWebDriver) getWebDriver()).getSessionId().toString());
            mobileNativePageFactory = createMobileNativePageFactory(pageobject);
        } else {
            // Launch remote browser and set it as the current thread
            webDriver.set(new RemoteWebDriver(
                    new URL(url),
                    capabilities));

            // set current sessionId
            sessionId.set(((RemoteWebDriver) getWebDriver()).getSessionId().toString());
            pageFactory = createPageFactory(pageobject);
        }

    }

    private PageFactory createPageFactory(String pageobject) {
        if (pageobject.equals("DesktopWeb")) {
            return new DesktopWebPageFactory();
        } else if (pageobject.equals("Android") || pageobject.equals("iOS")) {
            return new MobileWebPageFactory();
        }
        return null;
    }

    private MobileNativePageFactory createMobileNativePageFactory(String pageobject) {
        if (pageobject.contains("iOS")) {
            return new IosNativePageFactory();

        }
        return new AndroidNativePageFactory();
    }

    /**
     * Method that gets invoked after test.
     * Dumps browser log and
     * Closes the browser
     */
    @AfterMethod
    public void tearDown(ITestResult result) throws Exception {
        WebDriver driver = getWebDriver();
        Boolean status = result.isSuccess();
        if (driver != null) {
            if (TOUtils.isTO(driver)) {
                TOUtils.updateResults(getSessionId(), status);
            } else {
                SauceUtils.updateResults(driver, status);
            }

            driver.quit();
        }
    }

    protected void annotate(String text) {
        ((JavascriptExecutor) getWebDriver()).executeScript("sauce:context=" + text);
    }
}
