package org.warpten.wandrowid.network.battlenet;

import java.util.List;

/**
 * Created by perquet on 11/08/14.
 */
public final class BattlenetAuthChallenge extends BattlenetClientPacket {
    BattlenetAuthChallenge(String program, String platform, String locale, String login/*components*/) {
        super(new BattlenetPacketHeader(BattlenetPacketHeader.CMSG_AUTH_CHALLENGE,
                BattlenetPacketHeader.CHANNEL_AUTHENTICATION), new BattlenetBitStream());
    }

    public boolean Write(BattlenetSocket socket) {
        try {
            Stream.WriteFourCC(Program);
            Stream.WriteFourCC(Platform);
            Stream.WriteFourCC(Locale);

            Stream.Write(Components.size(), 6);
            for (BattlenetComponent component : Components) {
                Stream.WriteFourCC(component.Program);
                Stream.WriteFourCC(component.Platform);
                Stream.Write(component.Build, 32);
            }

            Stream.Write(1, 32); // Has Login
            // if (Has Login)
            Stream.WriteString(Login, 9, 3);
            return true;
        } catch (BattlenetBitStream.BitStreamException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean Interprete(BattlenetSocket socket)
    {
        if (!Write(socket))
            return false;

        socket.SendPacket(this);
        return true;
    }

    public String ToString() {
        String retVal = String.format("Battlenet::AuthChallenge: Program %s Platform %s Locale %s", Program, Platform, Locale);
        for (BattlenetComponent component : Components)
            retVal += String.format("%nBattlenet::Component: Program %s Platform %s Build %u",
                component.Program, component.Platform, component.Build);
        retVal += String.format("%nBattlenet::AuthChallenge: Login %s", Login);

        return retVal;
    }

    private String Program;
    private String Platform;
    private String Locale;
    private List<BattlenetComponent> Components;
    private String Login;
    private byte Region;
    private String GameAccountName;
}
