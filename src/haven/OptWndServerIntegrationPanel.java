package haven;

import haven.automated.mapper.MappingClient;

/**
 * Extracted Server Integration Settings panel builder for OptWnd.
 * Contains the widget construction logic that was previously inlined
 * in OptWnd.ServerIntegrationSettingsPanel.
 */
class OptWndServerIntegrationPanel {

	static void build(Widget panel, OptWnd.Panel back, OptWnd optWnd) {
		Widget prev;
		prev = panel.add(new Label("Web Map Integration"), 110, 8);
		prev = panel.add(new Label("Web Map Endpoint:"), prev.pos("bl").adds(0, 16).x(0));
		prev = panel.add(OptWnd.webmapEndpointTextEntry = new TextEntry(UI.scale(220), Utils.getpref("webMapEndpoint", "")){
			protected void changed() {
				Utils.setpref("webMapEndpoint", this.buf.line());
				MappingClient.destroy();
				super.changed();
			}
		}, prev.pos("ur").adds(6, 0));
		prev = panel.add(OptWnd.uploadMapTilesCheckBox = new CheckBox("Upload Map Tiles to your Web Map Server"){
			{a = Utils.getprefb("uploadMapTiles", false);}
			public void changed(boolean val) {
				Utils.setprefb("uploadMapTiles", val);
			}
		}, prev.pos("bl").adds(0, 8).x(12));
		OptWnd.uploadMapTilesCheckBox.tooltip = OptWndTooltips.uploadMapTiles;

		prev = panel.add(OptWnd.sendLiveLocationCheckBox = new CheckBox("Send Live Location to your Web Map Server"){
			{a = Utils.getprefb("enableLocationTracking", false);}
			public void changed(boolean val) {
				Utils.setprefb("enableLocationTracking", val);
			}
		}, prev.pos("bl").adds(0, 12));
		OptWnd.sendLiveLocationCheckBox.tooltip = OptWndTooltips.sendLiveLocation;

		prev = panel.add(new Label("Your Live Location Name (Req. Relog):"), prev.pos("bl").adds(20, 4));
		prev.tooltip = OptWndTooltips.liveLocationName;
		prev = panel.add(OptWnd.liveLocationNameTextEntry = new TextEntry(UI.scale(96), Utils.getpref("liveLocationName", "")){
			protected void changed() {
				Utils.setpref("liveLocationName", this.buf.line());
				super.changed();
			}
		}, prev.pos("ur").adds(6, 0));
		OptWnd.liveLocationNameTextEntry.tooltip = OptWndTooltips.liveLocationName;

		prev = panel.add(new Label("Cookbook Integration"), prev.pos("bl").adds(0, 26).x(110));
		prev = panel.add(new Label("Cookbook Endpoint:"), prev.pos("bl").adds(0, 16).x(0));
		prev = panel.add(OptWnd.cookBookEndpointTextEntry = new TextEntry(UI.scale(220), Utils.getpref("cookBookEndpoint", "")){
			protected void changed() {
				Utils.setpref("cookBookEndpoint", this.buf.line());
				super.changed();
			}
		}, prev.pos("ur").adds(6, 0));
		prev = panel.add(new Label("Cookbook Token:"), prev.pos("bl").adds(0, 8).x(0));
		prev = panel.add(OptWnd.cookBookTokenTextEntry = new TextEntry(UI.scale(220), Utils.getpref("cookBookToken", "")){
			protected void changed() {
				Utils.setpref("cookBookToken", this.buf.line());
				super.changed();
			}
		}, prev.pos("ur").adds(20, 0));

		Widget backButton;
		panel.add(backButton = optWnd.new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 26).x(0));
		panel.pack();
		optWnd.centerBackButton(backButton, panel);
	}
}
