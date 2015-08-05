/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.csipsimple.utils;

import java.util.Locale;

import com.csipsimple.wizards.WizardUtils.WizardInfo;
import com.csipsimple.wizards.impl.EZCallPrepay;
import tw.net.ezcall.R;

public final class CustomDistribution {

	private CustomDistribution() {
	}

	// CSipSimple trunk distribution
	/**
	 * Does this distribution allow to create other accounts than the one of the
	 * distribution
	 * 
	 * @return Whether other accounts can be created
	 */
	public static boolean distributionWantsOtherAccounts() {
		return true;
		// return false;
	}

	/**
	 * Does this distribution allow to list other providers in other accounts
	 * creation
	 * 
	 * @return Whether other provider are listed is wizard picker
	 */
	public static boolean distributionWantsOtherProviders() {
		// return true;
		return false;
	}

	/**
	 * Email address for support and feedback If none return the feedback
	 * feature is disabled
	 * 
	 * @return the email address of support
	 */
	public static String getSupportEmail() {
		// return "developers@csipsimple.com";
		return "developers@ezcall.net.tw";
	}

	/**
	 * SIP User agent to send by default in SIP messages (by default device
	 * infos are added to User Agent string)
	 * 
	 * @return the default user agent
	 */
	public static String getUserAgent() {
		// return "CSipSimple";
		return "EZCall";
	}

	/**
	 * The default wizard info for this distrib. If none no custom distribution
	 * wizard is shown
	 * 
	 * @return the default wizard info
	 */
	public static WizardInfo getCustomDistributionWizard() {
		// return null;
		return new WizardInfo("EZCallPrepay", "EZCall Prepay",
				R.drawable.ic_wizard_ezcall, 3, 
				new Locale[] {}, true, false,
				EZCallPrepay.class);
	}

	/**
	 * Show or not the issue list in help
	 * 
	 * @return whether link to issue list should be displayed
	 */
	public static boolean showIssueList() {
		return false;
	}

	/**
	 * Get the link to the FAQ. If null or empty the link to FAQ is not
	 * displayed
	 * 
	 * @return link to the FAQ
	 */
	public static String getFaqLink() {
		// return
		// "http://code.google.com/p/csipsimple/wiki/FAQ?show=content,nav#Summary";
		//return "http://www.ezcall.net.tw/support/FAQ.asp";
		return "http://www.fa-phone.com/news_detail.php?id=1";
	}

	/**
	 * Whether we want to display first fast setting screen to allow user to
	 * quickly configure the sip client
	 * 
	 * @return true if the fast setting screen should be displayed
	 */
	public static boolean showFirstSettingScreen() {
		// return true;
		return false;
	}

	/**
	 * Do we want to display messaging feature
	 * 
	 * @return true if the feature is enabled in this distribution
	 */
	public static boolean supportMessaging() {
		return false;
	}

	/**
	 * Do we want to display the favorites feature
	 * 
	 * @return true if the feature is enabled in this distribution
	 */
	public static boolean supportFavorites() {
		return false;
	}

	/**
	 * Do we want to display record call option while in call If true the record
	 * of conversation will be enabled both in ongoing call view and in settings
	 * as "auto record" feature
	 * 
	 * @return true if the feature is enabled in this distribution
	 */
	public static boolean supportCallRecord() {
		return true;
	}

	/**
	 * Shall we force the no mulitple call feature to be set to false
	 * 
	 * @return true if we don't want to support multiple calls at all.
	 */
	public static boolean forceNoMultipleCalls() {
		// return false;
		return true;
	}

	/**
	 * Should the wizard list display a given generic wizard
	 * 
	 * @param wizardTag
	 *            the tag of the generic wizard
	 * @return true if you'd like the wizard to be listed
	 */
	public static boolean distributionWantsGeneric(String wizardTag) {
		// return true;
		return false;
	}

	/**
	 * Get the SD card folder name. This folder will be used to store call
	 * records, configs and logs
	 * 
	 * @return the name of the folder to use
	 */
	public static String getSDCardFolder() {
		// return "CSipSimple";
		return "EZCall";
	}

	/**
	 * Get the complete URL for the nightly updater. Return null or "" to
	 * disable. Check NightlyUpdater.java for the full URL and how the server
	 * should respond...
	 * 
	 * @return
	 */
	public static String getNightlyUpdaterURL() {
		// return "http://nightlies.csipsimple.com/";
		return null;
	}
}
