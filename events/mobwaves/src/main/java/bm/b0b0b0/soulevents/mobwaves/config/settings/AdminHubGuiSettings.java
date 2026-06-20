package bm.b0b0b0.soulevents.mobwaves.config.settings;



import net.elytrium.serializer.annotations.Comment;

import net.elytrium.serializer.annotations.CommentValue;

import net.elytrium.serializer.language.object.YamlSerializable;

import org.bukkit.Material;



public final class AdminHubGuiSettings extends YamlSerializable {



    public int rows = 4;

    public int backSlot = 0;

    public String backMaterial = Material.LIGHT_GRAY_DYE.name();

    public int profileListStartSlot = 18;

    public int waveProfilesSlot = 4;

    public String waveProfilesMaterial = Material.NETHER_STAR.name();

    public int typeListStartSlot = 10;

    public String defaultProfileIconMaterial = Material.NETHER_STAR.name();

}

