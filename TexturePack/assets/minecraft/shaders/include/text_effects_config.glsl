TEXT_EFFECT(240, 240, 0) {
apply_growing_movement(1.0);
apply_camo(
    rgb(170, 20, 43),   
    rgb(253, 130, 142),  
    rgb(230, 210, 210), 
    rgb(250, 245, 245),
    45.0);
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 4) {
    apply_waving_movement();
    apply_animated_gradient_7_smoother(rgb(255, 95, 31), rgb(255, 139, 61), rgb(255, 172, 72), rgb(255, 110, 50), rgb(255, 73, 0), rgb(215, 85, 29), rgb(175, 68, 20));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 8) {
    apply_iterating_movement(2.5, 1.0); // 2x faster
    apply_rainbow_fractal();
    apply_rainbow_abberation(0.42, 0.84);  // speed and intensity
    apply_shimmer(0.6, 0.45);	
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 12) {
    apply_crack_movement(1.0, 1.5);
    apply_noxs_veil_rainbow(4.0); 
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 16) {
    apply_melting_movement(1.0, 2.0);
    apply_camo(
        rgb(226, 255, 254),
        rgb(151, 243, 255),
        rgb(49, 248, 255),  
        rgb(38, 176, 255), 
        50.0);
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 20) {
    override_text_color(rgb(255, 82, 82));
    apply_outline(rgb(100, 20, 80));
}

TEXT_EFFECT(240, 240, 24) {
    apply_gradient(rgb(255, 235, 120), rgb(255, 82, 82));
}

TEXT_EFFECT(240, 240, 28) {
    apply_rainbow();
}

TEXT_EFFECT(240, 240, 32) {
    override_text_color(rgb(86, 235, 86));
    override_shadow_color(rgb(20, 80, 90));
	apply_shimmer(0.6, 0.45);	
}

TEXT_EFFECT(240, 240, 36) {
    override_text_color(rgb(255, 255, 255));
	apply_chromatic_abberation(0.35, 0.7, vec4(1.0, 1.0, 1.0, 1.0), vec4(0.0, 0.0, 0.0, 1.0));	
    apply_glitch(0.2, 0.75); // Moderate intensity and speed for clear visibility
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 40) {
apply_noxs_veil(
    vec3(0.3, 0.3, 0.3),    // color1 - Medium gray
    vec3(0.5, 0.5, 0.5),    // color2 - Gray
    vec3(0.8, 0.8, 0.8),    // color3 - Light gray
    vec3(1.0, 1.0, 1.0),    // color4 - White
    3.5                      // speed
);
    apply_shaking_movement();
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 44) {
    override_text_color(rgb(255, 20, 20));
    apply_fire();
    apply_fade(rgb(238, 121, 12));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 48) {
    apply_heart(
        rgb(250, 4, 17),
        rgb(235, 20, 31),
        rgb(255, 137, 137), 
        rgb(255, 255, 255), 
        0.475
    );
    apply_chromatic_abberation(0.30, 0.60, vec4(202.0/255.0, 12.0/255.0, 22.0/255.0, 1.0), vec4(255.0/255.0, 150.0/255.0, 155.0/255.0, 1.0));
    apply_shaking_movement();
    apply_waving_movement();
	apply_shimmer(0.6, 0.45);	
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 52) {
    apply_heart(
        rgb(255, 100, 150),
        rgb(255, 50, 100),  
        rgb(255, 150, 180), 
        rgb(255, 255, 255), 
        0.475
    );
    apply_chromatic_abberation(0.30, 0.60, vec4(1.0, 0.2, 0.6, 1.0), vec4(0.9, 0.0, 0.9, 1.0));
    apply_shaking_movement();
    apply_waving_movement();
	apply_shimmer(0.6, 0.45);	
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 56) {
    override_text_color(rgb(235, 235, 235));
    override_shadow_color(rgb(0, 0, 0));
    apply_outline(rgb(0, 0, 0));
    apply_blinking();
}

TEXT_EFFECT(240, 240, 60) {
    apply_spiral_embrace(0.5, 2.0);
    apply_animated_gradient_3(rgb(31, 31, 31), rgb(125, 125, 125), rgb(255, 255, 255));
    apply_outline(rgb(0, 0, 0));
    apply_glitch(0.15, 0.5); // Moderate intensity and speed for clear visibility
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 64) {
    apply_shimmer(0.8, 0.55);	
}

TEXT_EFFECT(240, 240, 68) {
    apply_dangling(1.0, 3.0);	
    apply_pale_rainbow_fractal();
    apply_pastel_outline();
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 72) {
    apply_sliding_movement(10.0);
    apply_pastel_camo(37.5, 5.0);
    apply_shimmer(0.5, 0.30);	
    apply_waving_movement(0.5, 0.75);
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 76) {
    apply_shake_movement(50.0);
apply_camo(
    rgb(251, 255, 226),
    rgb(245, 255, 151),
    rgb(255, 252, 81),  
    rgb(255, 215, 38), 
    40.0);
apply_camo_outline(
    vec3(251.0/255.0, 255.0/255.0, 226.0/255.0),
    vec3(245.0/255.0, 255.0/255.0, 151.0/255.0),
    vec3(255.0/255.0, 252.0/255.0, 81.0/255.0),
    vec3(255.0/255.0, 215.0/255.0, 38.0/255.0),
    40.0);
    apply_egg_crack(0.15, 0.35);
    apply_glitch(0.15, 0.35); // Moderate intensity and speed for clear visibility
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 80) {
apply_noxs_veil(
vec3(0.82, 0.82, 0.86),  // color1
vec3(0.88, 0.88, 0.91),  // color2
vec3(0.93, 0.93, 0.96),  // color3
vec3(0.97, 0.97, 1.00),  // color4
    3.5
);
apply_hopping_movement(5.0);
    apply_dangling_global(1.0, 3.0);	
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 84) {
    apply_pansexual_pride();
}

TEXT_EFFECT(240, 240, 88) {
    apply_asexual_pride();
}

TEXT_EFFECT(240, 240, 92) {
    apply_aromantic_pride();
}

TEXT_EFFECT(240, 240, 96) {
    apply_non_binary_pride();
}

TEXT_EFFECT(240, 240, 100) { // green
    apply_animated_gradient_3(rgb(0, 230, 118), rgb(118, 255, 3), rgb(178, 255, 89));
}

TEXT_EFFECT(240, 240, 104) { // blue
    apply_animated_gradient_3(rgb(37, 70, 232), rgb(37, 135, 232), rgb(76, 205, 245));
}

TEXT_EFFECT(240, 240, 108) { // yellow
    apply_animated_gradient_3(rgb(255, 145, 0), rgb(255, 255, 0), rgb(255, 255, 141));
}

TEXT_EFFECT(240, 240, 112) { // purple
    apply_animated_gradient_3(rgb(136, 59, 252), rgb(224, 84, 229), rgb(143, 68, 232));
}

TEXT_EFFECT(240, 240, 116) { // pink
    apply_animated_gradient_3(rgb(255, 41, 149), rgb(255, 58, 243), rgb(247, 115, 255));
}

TEXT_EFFECT(240, 240, 120) { // red
    apply_animated_gradient_3(rgb(213, 0, 0), rgb(245, 0, 87), rgb(255, 138, 128));
}

TEXT_EFFECT(240, 240, 124) { // dreamteam
    apply_animated_gradient_3(rgb(168, 131, 243), rgb(131, 147, 249), rgb(222, 126, 246));
}

TEXT_EFFECT(240, 240, 128) { // cosmicquartz
    apply_animated_gradient_3(rgb(28, 242, 149), rgb(51, 255, 255), rgb(255, 0, 255));
}

TEXT_EFFECT(240, 240, 132) { // easter
    apply_animated_gradient_3(rgb(255, 204, 255), rgb(204, 255, 204), rgb(204, 255, 255));
}

TEXT_EFFECT(240, 240, 136) {
    apply_pastel_rainbow();
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 140) {
    apply_red_fractal();
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 144) {
    apply_pink_fractal();
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 148) { // icy aurora
    apply_animated_gradient_7_smoother(rgb(173, 216, 230), rgb(135, 206, 235), rgb(0, 206, 209), rgb(64, 224, 208), rgb(0, 191, 255), rgb(175, 238, 238), rgb(176, 224, 230));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 152) { // mint delight
    apply_animated_gradient_7_smoother(rgb(152, 251, 152), rgb(144, 238, 144), rgb(0, 255, 127), rgb(0, 250, 154), rgb(102, 205, 170), rgb(127, 255, 212), rgb(143, 188, 143));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 156) { // summer sunshine
    apply_animated_gradient_7_smoother(rgb(255, 14, 14), rgb(253, 108, 26), rgb(255, 148, 50), rgb(255, 204, 24), rgb(255, 246, 39), rgb(255, 118, 60), rgb(231, 59, 59));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 160) { // wizard magic
    apply_animated_gradient_7_smoother(rgb(105, 42, 239), rgb(95, 92, 253), rgb(139, 39, 239), rgb(205, 62, 255), rgb(255, 131, 243), rgb(208, 90, 90), rgb(246, 53, 53));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 164) { // ocean blue
    apply_animated_gradient_7_smoother(rgb(24, 144, 255), rgb(88, 129, 235), rgb(142, 82, 233), rgb(98, 102, 238), rgb(64, 183, 232), rgb(43, 203, 186), rgb(71, 85, 243));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 168) { // pumpkin patch
    apply_animated_gradient_7_smoother(rgb(255, 95, 31), rgb(255, 139, 61), rgb(255, 172, 72), rgb(255, 110, 50), rgb(255, 73, 0), rgb(215, 85, 29), rgb(175, 68, 20));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 172) { // witch brew
    apply_animated_gradient_7_smoother(rgb(148, 0, 211), rgb(186, 85, 211), rgb(147, 112, 219), rgb(170, 143, 226), rgb(111, 45, 168), rgb(153, 50, 204), rgb(120, 81, 169));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 176) { // vampire kiss
    apply_animated_gradient_7_smoother(rgb(178, 34, 34), rgb(220, 20, 60), rgb(139, 0, 0), rgb(184, 15, 10), rgb(229, 36, 36), rgb(255, 0, 0), rgb(97, 6, 6));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 180) { // electric pulse
    apply_animated_gradient_7_smoother(rgb(75, 0, 130), rgb(148, 0, 211), rgb(255, 0, 255), rgb(138, 43, 226), rgb(255, 20, 147), rgb(255, 0, 255), rgb(186, 85, 211));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 184) { // spring meadow
    apply_animated_gradient_7_smoother(rgb(100, 253, 152), rgb(34, 214, 18), rgb(144, 238, 144), rgb(211, 254, 39), rgb(152, 251, 152), rgb(30, 162, 19), rgb(127, 255, 0));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 188) { // golden sparkle
    apply_animated_gradient_7_smoother(rgb(255, 215, 0), rgb(255, 223, 0), rgb(255, 193, 37), rgb(218, 165, 32), rgb(240, 230, 140), rgb(255, 239, 213), rgb(184, 134, 11));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 192) { // candy cane
    apply_animated_gradient_7_smoother(rgb(255, 0, 0), rgb(255, 182, 193), rgb(220, 20, 60), rgb(255, 192, 203), rgb(255, 105, 180), rgb(240, 128, 128), rgb(205, 92, 92));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 196) { // poinsettia bloom
    apply_animated_gradient_7_smoother(rgb(199, 21, 133), rgb(255, 105, 180), rgb(208, 32, 144), rgb(219, 112, 147), rgb(231, 84, 128), rgb(255, 20, 147), rgb(176, 48, 96));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 200) { // sweet embrace
    apply_animated_gradient_7_smoother(rgb(255, 182, 193), rgb(255, 160, 122), rgb(255, 105, 180), rgb(255, 69, 105), rgb(219, 112, 147), rgb(255, 218, 185), rgb(250, 128, 114));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 204) { // enchanted evening
    apply_animated_gradient_7_smoother(rgb(93, 48, 167), rgb(72, 61, 139), rgb(123, 104, 238), rgb(147, 112, 219), rgb(186, 85, 211), rgb(255, 105, 180), rgb(176, 48, 96));
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 208) { // scarlet passion
    apply_character_cycle(rgb(214, 29, 95), rgb(253, 100, 228), 5.0, 0.3); // Love
}

TEXT_EFFECT(240, 240, 212) { // rainbow_glitch
    apply_rainbow_fractal();
    apply_rainbow_abberation(0.42, 0.84);  // speed and intensity
    apply_shimmer(0.6, 0.45);	
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 216) { // pale rainbow fractal
    apply_pale_rainbow_fractal();
	apply_shimmer(1.0, 0.7);	
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 220) { // eastershimmer
    apply_character_cycle(rgb(255, 102, 255), rgb(255, 255, 102), 10.0, 0.6);
    apply_color_shimmer(vec3(1.0, 1.0, 1.0), 0.8, 0.52);
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 224) { // yinyang
    apply_character_cycle(rgb(60, 60, 60), rgb(255, 255, 255), 4.5, 0.325);
	apply_chromatic_abberation(0.25, 0.5, vec4(1.0, 1.0, 1.0, 1.0), vec4(0.0, 0.0, 0.0, 1.0));	
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 228) { // candyfloss
    apply_character_cycle_three_colors(
        rgb(154, 224, 252), // baby blue (blue)
        rgb(253, 168, 246), // baby pink (pink)
        rgb(250, 253, 255), // pale (white)
        5.0, 0.4
    );  
    apply_color_shimmer(vec3(1.0, 1.0, 1.0), 0.4, 0.2);
    textData.shouldScale = true;
}

TEXT_EFFECT(240, 240, 232) { // spring garden
    apply_animated_gradient_7_smoother(
        rgb(255, 192, 203), // pink tulip
        rgb(255, 255, 153), // daffodil yellow
        rgb(144, 238, 144), // light green
        rgb(173, 216, 230), // sky blue
        rgb(221, 160, 221), // plum
        rgb(255, 222, 173), // soft beige
        rgb(240, 230, 140)  // warm yellow
    );
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 5) { // gradyinyang
    apply_animated_gradient_3(rgb(75, 75, 75), rgb(125, 125, 125), rgb(255, 255, 255));
    apply_chromatic_abberation(0.3, 0.6, vec4(1.0, 1.0, 1.0, 1.0), vec4(0.0, 0.0, 0.0, 1.0));	
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 10) { // gradeasteregg
    apply_animated_gradient_7_smoother(rgb(255, 102, 255), rgb(255, 255, 102), rgb(255, 102, 255), rgb(255, 255, 102), rgb(255, 102, 255), rgb(255, 255, 102), rgb(255, 255, 102));
    apply_color_shimmer(vec3(1.0, 1.0, 1.0), 0.8, 0.52);
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 15) { // gradcottoncandy
    apply_animated_gradient_3(rgb(154, 224, 252), rgb(253, 168, 246), rgb(250, 253, 255));
    apply_color_shimmer(vec3(1.0, 1.0, 1.0), 0.4, 0.2);
    textData.shouldScale = true;
}

 TEXT_EFFECT(245, 245, 20) { // peach summer
    apply_shimmer(0.52, 0.42);	
apply_animated_gradient_7_smoother(
  rgb(219, 112, 180),  /* soft magenta */
  rgb(245, 136, 69),   /* orange sunset */
  rgb(245, 69, 127),   /* hot pink */
  rgb(245, 136, 69),    /* warm red */
  rgb(237, 235, 85),   /* orange again for cohesion */
  rgb(219, 112, 180),  /* magenta again */
  rgb(245, 69, 127)    /* pink again to close loop */
);
    textData.shouldScale = true;
}

 TEXT_EFFECT(245, 245, 25) { // mtndewprefix
    apply_animated_gradient_5(rgb(60, 250, 187), rgb(168, 253, 210), rgb(196, 236, 223), rgb(13, 233, 141), rgb(194, 255, 209));
	apply_shimmer(0.6, 0.45);	
    textData.shouldScale = true;
}

 TEXT_EFFECT(245, 245, 30) { // mtndewchat
    apply_shimmer(0.52, 0.42);	
   apply_character_cycle_four_colors(
        rgb(60, 250, 187), // baby blue (blue)
        rgb(168, 253, 210), // baby pink (pink)
        rgb(196, 236, 223), // pale (white)
        rgb(13, 233, 141), // pale (white)
        5.0, 0.4
    );  
    textData.shouldScale = true;
}

 TEXT_EFFECT(245, 245, 35) { // water
    apply_character_cycle(rgb(0, 110, 255), rgb(0, 204, 255), 7.5, 0.5); // Love
    textData.shouldScale = true;
}

 TEXT_EFFECT(245, 245, 40) { // beachball or retro disco
    apply_shimmer(0.52, 0.42);	
apply_animated_gradient_7_smoother(rgb(255, 61, 113), rgb(255, 158, 0), rgb(255, 234, 0), rgb(0, 229, 255), rgb(17, 153, 255), rgb(134, 77, 255), rgb(255, 61, 113));
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 45) { // america
    apply_animated_gradient_5(rgb(75, 55, 255), rgb(255, 0, 0), rgb(255, 255, 255), rgb(75, 55, 255), rgb(255, 0, 0));
    apply_shimmer(0.45, 0.37);	
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 50) { // flame
    apply_animated_gradient_7_smoother(rgb(255, 66, 45), rgb(255,111,0), rgb(255,165,0), rgb(255,200,87), rgb(255,80,0), rgb(202, 17, 17), rgb(247, 109, 4));
	apply_chromatic_abberation(0.39, 0.78, vec4(1.0, 0.3, 0.0, 1.0), vec4(0.8, 0.0, 0.0, 1.0));
    apply_shimmer(0.52, 0.42);	
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 55) { // devils nightmare (red)
    apply_animated_gradient_7_smoother(
        rgb(200, 0, 0),     // blood red
        rgb(140, 0, 0),     // dark crimson
        rgb(255, 90, 30),   // fiery orange
        rgb(109, 0, 0),      // almost black red
        rgb(255, 40, 40),   // fresh blood
        rgb(180, 30, 0),    // burnt scarlet
        rgb(68, 0, 0)       // deep black-red
    );
    textData.shouldScale = true;
} 

TEXT_EFFECT(245, 245, 60) { // midnight_curse (purple/black)
    apply_animated_gradient_7_smoother(
        rgb(87, 42, 143),  rgb(69, 42, 170),  rgb(76, 31, 180),
        rgb(90,30,180), rgb(140,60,255), rgb(92, 18, 167),
        rgb(95, 47, 153)
    );
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 65) { // halloween night (purple/orange)
    apply_animated_gradient_7_smoother(
        rgb(120, 30, 200),  // deep purple
        rgb(180, 60, 255),  // bright purple
        rgb(255, 140, 0),   // pumpkin orange
        rgb(230, 100, 20),  // burnt orange
        rgb(255, 190, 60),  // candy corn orange
        rgb(200, 80, 255),   // vibrant spooky violet
        rgb(100, 20, 160)  // shadow purple
    );
    textData.shouldScale = true;
} 

TEXT_EFFECT(245, 245, 70) { // witch poison (green/red)
    apply_animated_gradient_7_smoother(
        rgb(120, 0, 180),   // dark arcane purple
        rgb(180, 80, 255),  // glowing violet
        rgb(60, 200, 90),   // toxic green
        rgb(20, 140, 60),   // swamp green
        rgb(0, 255, 120),   // neon slime green
        rgb(150, 50, 220),  // bright witch purple
        rgb(90, 0, 130)     // shadow purple
    );
    apply_shimmer(0.48, 0.39);
    textData.shouldScale = true;
} 

TEXT_EFFECT(245, 245, 75) { // night sky
    apply_fractal_color(
        rgb(16, 12, 236),
        rgb(98, 21, 243), 
        rgb(146, 64, 240),
        rgb(195, 48, 253),
        rgb(146, 64, 240),
        rgb(98, 21, 243), 
        rgb(16, 12, 236),
        0.85                 // speed (0.5 = half speed, 2.0 = double speed)
    );
    apply_chromatic_abberation(0.33, 0.66, vec4(0.0, 0.2, 0.6, 1.0), vec4(0.6, 0.0, 0.8, 1.0));
    textData.shouldScale = true;
} 

TEXT_EFFECT(245, 245, 80) { // ghost
    apply_animated_gradient_7_smoother(
        rgb(22, 62, 87),     // 163E57 - deep teal
        rgb(36, 130, 130),   // 248282 - medium teal
        rgb(141, 252, 252),  // 8dfcfc - bright cyan
        rgb(65, 191, 191),   // 41BFBF - turquoise
        rgb(22, 62, 87),     // 163E57 - deep teal
        rgb(36, 130, 130),   // 248282 - medium teal
        rgb(141, 252, 252)  // 8dfcfc - bright cyan
    );
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 85) { // volcano
    apply_animated_gradient_7_smoother(
    rgb(230, 66, 25), // magic rose
    rgb(120, 4, 4), // spell violet
    rgb(255, 43, 28), // enchanted blue
    rgb(245, 174, 7), // mana haze
    rgb(245, 174, 7), // mana haze
    rgb(120, 4, 4), // spell violet
    rgb(245, 59, 7) // enchanted blue
);
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 90) { // aurora
    apply_animated_gradient_7_smoother(
    rgb(86, 16, 218), // magic rose
    rgb(167, 65, 235), // spell violet
    rgb(18, 219, 226), // enchanted blue
    rgb(8, 196, 165), // mana haze
    rgb(18, 219, 226), // enchanted blue
    rgb(167, 65, 235), // spell violet
    rgb(86, 16, 218) // magic rose
);
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 95) { // pink blue
    apply_fractal_color(
        rgb(206, 44, 152),
        rgb(247, 4, 214), 
        rgb(138, 238, 255),
        rgb(83, 229, 255),
        rgb(138, 238, 255),
        rgb(247, 4, 214), 
        rgb(206, 44, 152),
        0.85                 // speed (0.5 = half speed, 2.0 = double speed)
    );
    apply_chromatic_abberation(0.33, 0.66, vec4(1.0, 0.2, 0.6, 1.0), vec4(0.9, 0.0, 0.9, 1.0)); // Added semicolon here
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 100) { // rainbow nox
    apply_noxs_veil_rainbow(2.5);  // speed parameter controls animation speed
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 105) { // pastel outline fractal
    apply_pale_rainbow_fractal();
    apply_pastel_outline();
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 110) { // winter camo
    apply_camo(
        rgb(226, 255, 254),
        rgb(151, 243, 255),
        rgb(49, 248, 255),  
        rgb(38, 176, 255), 
        25.0);
    apply_camo_outline(
    vec3(226.0/255.0, 255.0/255.0, 254.0/255.0),
    vec3(151.0/255.0, 243.0/255.0, 255.0/255.0),
    vec3(49.0/255.0, 248.0/255.0, 255.0/255.0),
    vec3(38.0/255.0, 176.0/255.0, 255.0/255.0),
    50.0
);
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 115) { // cookie monster
    apply_camo(
        rgb(255, 228, 196),  // sugar/butter (light cream)
        rgb(222, 184, 135),  // cookie dough (tan)
        rgb(205, 133, 63),   // baked cookie (golden brown)
        rgb(181, 101, 29),   // chocolate chips (dark brown)
        37.5);
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 120) { // firework
    apply_animated_gradient_5(
        rgb(252, 56, 141), // pink tulip
        rgb(91, 46, 255), // daffodil yellow
        rgb(145, 39, 106), // light green
        rgb(255, 0, 0), // sky blue
        rgb(255, 238, 66) // plum
    );
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 125) { // christmas tree
apply_animated_gradient_6_smoother(
    rgb(133, 0, 0),    // earthy red
    rgb(253, 30, 30),    // warm red tone
    rgb(252, 119, 119),  // soft red
    rgb(1, 70, 1),     // deeper forest
    rgb(3, 160, 3),    // classic green (soft)
    rgb(157, 238, 157)  // pastel green
);
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 130) { // starlit heart
    apply_heart(
        rgb(255, 100, 150),
        rgb(255, 50, 100),  
        rgb(255, 150, 180), 
        rgb(255, 255, 255), 
        0.475
    );
    apply_chromatic_abberation(0.30, 0.60, vec4(1.0, 0.2, 0.6, 1.0), vec4(0.9, 0.0, 0.9, 1.0));
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 135) { // lunar love
apply_fractal_color(
    rgb(196, 0, 0),    // Deep red
    rgb(133, 26, 255),   // Magenta pink
    rgb(255, 85, 204),  // Pink
    rgb(255, 255, 255),  // White
    rgb(255, 85, 204),  // Pink
    rgb(133, 26, 255),   // Magenta pink
    rgb(196, 0, 0),    // Deep red
    0.5
);
apply_chromatic_abberation(0.33, 0.66, vec4(1.0, 0.0, 0.7, 1.0), vec4(0.47, 0.0, 1.0, 1.0));
    textData.shouldScale = true;
} 

TEXT_EFFECT(245, 245, 140) { // cherry blossom
    apply_animated_gradient_7_smoother(
        rgb(255, 120, 200), // retro bubblegum
        rgb(255, 70, 180),  // hot retro pink
        rgb(255, 0, 150),   // neon pink core
        rgb(255, 40, 170),  // synthwave pink
        rgb(255, 90, 195),  // arcade pink
        rgb(255, 150, 215), // soft retro glow
        rgb(255, 60, 185)   // candy neon
    );
    textData.shouldScale = true;
} 

TEXT_EFFECT(245, 245, 145) { // blush kiss
    apply_animated_gradient_7_smoother(rgb(167, 18, 1), rgb(255, 0, 0), rgb(241, 89, 255), rgb(252, 166, 255), rgb(241,89,255), rgb(255, 0, 0), rgb(167, 18, 1));
    textData.shouldScale = true;
} 

TEXT_EFFECT(245, 245, 150) { // sweet embrace
    apply_animated_gradient_6_smoother(rgb(131, 129, 255), rgb(163, 161, 255), rgb(227, 136, 255), rgb(255, 145, 174), rgb(252, 160, 240), rgb(255, 174, 195));
    textData.shouldScale = true;
} 

TEXT_EFFECT(245, 245, 155) { // spring light &x&f&5&f&5&9&b
apply_animated_gradient_6_smoother(
    rgb(255, 147, 194),
    rgb(255, 170, 210),
    rgb(129, 219, 162),
    rgb(162, 233, 183),
    rgb(142, 186, 248),
    rgb(175, 207, 247));
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 160) { // dew haze &x&f&5&f&5&a&0
apply_animated_gradient_6_smoother(
    rgb(80, 190, 100),
    rgb(130, 225, 140),
    rgb(50, 170, 200),
    rgb(100, 210, 230),
    rgb(190, 230, 80),
    rgb(220, 250, 130));
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 165) { // forest spirit &x&f&5&f&5&a&5
apply_camo(
        rgb(197, 255, 212),
        rgb(48, 219, 91),
        rgb(0, 160, 40),
        rgb(0, 88, 22),
        37.5);
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 170) { // dreamy cloud &x&f&5&f&5&a&a
    apply_pastel_camo(37.5, 5.0);
    apply_pastel_camo_outline(37.5, 5.0);
    apply_shimmer(0.5, 0.30);	
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 175) { // crystal pulse &x&f&5&f&5&a&f
    apply_fractal_color(
        rgb(172, 109, 255),
        rgb(150, 255, 255),
        rgb(255, 145, 185),
        rgb(255, 210, 90),
        rgb(172, 109, 255),
        rgb(150, 255, 181),
        rgb(255, 210, 90),
        0.50 
    );
    apply_fractal_color_outline(
        rgb(172, 109, 255),
        rgb(150, 255, 255),
        rgb(255, 145, 185),
        rgb(255, 210, 90),
        rgb(172, 109, 255),
        rgb(150, 255, 181),
        rgb(255, 210, 90),
        0.50
    );
    apply_color_shimmer(vec3(1.0, 0.88, 0.4), 0.55, 0.38);
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 180) { // celestial
apply_fractal_color(
    rgb(86, 16, 218), // magic rose
    rgb(167, 65, 235), // spell violet
    rgb(18, 219, 226), // enchanted blue
    rgb(8, 196, 165), // mana haze
    rgb(18, 219, 226), // enchanted blue
    rgb(167, 65, 235), // spell violet
    rgb(86, 16, 218), // magic rose
    0.5
);
apply_fractal_color_outline(
    rgb(86, 16, 218), // magic rose
    rgb(167, 65, 235), // spell violet
    rgb(18, 219, 226), // enchanted blue
    rgb(8, 196, 165), // mana haze
    rgb(18, 219, 226), // enchanted blue
    rgb(167, 65, 235), // spell violet
    rgb(86, 16, 218), // magic rose
    0.5
);
    textData.shouldScale = true;
} 

TEXT_EFFECT(245, 245, 185) { // celestial
apply_fractal_color(
    rgb(255, 195, 32),   // deep gold
    rgb(240, 208, 96),   // bright gold
    rgb(255, 246, 198),  // warm white
    rgb(255, 255, 255),  // pure white
    rgb(255, 251, 232),  // warm white
    rgb(240, 208, 96),   // bright gold
    rgb(255, 195, 32),   // deep gold
    0.5
);
apply_fractal_color_outline(
    rgb(255, 195, 32),   // deep gold
    rgb(240, 208, 96),   // bright gold
    rgb(255, 246, 198),  // warm white
    rgb(255, 255, 255),  // pure white
    rgb(255, 251, 232),  // warm white
    rgb(240, 208, 96),   // bright gold
    rgb(255, 195, 32),   // deep gold
    0.5
);
    textData.shouldScale = true;
}

TEXT_EFFECT(245, 245, 190) { // nexus
apply_fractal_color(
    rgb(120, 40, 0),     // deep dark brown
    rgb(200, 80, 10),    // burnt orange
    rgb(255, 140, 30),   // bright orange
    rgb(255, 229, 80),   // light orange highlight
    rgb(255, 140, 30),   // bright orange
    rgb(200, 80, 10),    // burnt orange
    rgb(120, 40, 0),     // deep dark brown
    0.5
);
apply_fractal_color_outline(
    rgb(120, 40, 0),     // deep dark brown
    rgb(200, 80, 10),    // burnt orange
    rgb(255, 140, 30),   // bright orange
    rgb(255, 229, 80),   // light orange highlight
    rgb(255, 140, 30),   // bright orange
    rgb(200, 80, 10),    // burnt orange
    rgb(120, 40, 0),     // deep dark brown
    0.5
);
    textData.shouldScale = true;
}

TEXT_EFFECT(94, 171, 136) {
    apply_waving_movement(1.0, 1.5);
    apply_gradient(rgb(189, 221, 100), rgb(50, 117, 132));
    override_shadow_color(rgb(70, 70, 100));
}

TEXT_EFFECT(255, 255, 248) {
    apply_vertical_shadow();
    apply_metalic(rgb(255, 255, 255), rgb(150, 163, 177) * 0.95);
    override_shadow_color(rgb(70, 70, 100));
}