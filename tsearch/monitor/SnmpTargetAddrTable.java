/*
 * * Copyright (C) 2002-2004 Rui Pedro Lopes (rlopes at ipb dot pt) *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 *
 */

package ro.cst.tsearch.monitor;

import java.util.Hashtable;
import java.util.StringTokenizer;

import pt.ipb.agentapi.AbstractAgent;
import pt.ipb.agentapi.AbstractConceptualTableModel;
import pt.ipb.agentapi.ConceptualTableProvider;
import pt.ipb.agentapi.ConceptualTableRow;
import pt.ipb.agentapi.TableProvider;
import pt.ipb.agentapi.TableRow;
import pt.ipb.agentapi.event.ControlAdapter;
import pt.ipb.agentapi.event.ControlEvent;
import pt.ipb.snmp.type.smi.Int;
import pt.ipb.snmp.type.smi.OID;
import pt.ipb.snmp.type.smi.OctetString;
import pt.ipb.snmp.type.smi.StringOID;
import pt.ipb.snmp.type.smi.Var;
import pt.ipb.snmp.type.tc.Str;

import org.apache.log4j.Logger;

class TargetProvider extends ControlAdapter implements ConceptualTableProvider {
	
	private static final Logger logger = Logger.getLogger(TargetProvider.class);
	
    Var values[] = null;

  public TargetProvider() {
    values = new Var[8];
  }

  public void activate(ControlEvent e) {
    ConceptualTableRow row = (ConceptualTableRow) e.getSource();
    String key = row.getKey().toString();
    String tagList = Str.toString((OctetString) getValueAt(4));
    logger.info("ControlRequested on TargetTable-" + e.getCommand());
  }

  public int length() {
    return values.length;
  }

  public void setValueAt(int i, Var v) {
    values[i] = v;
  }

  public Var getValueAt(int i) {
    return values[i];
  }

  public int getRowStatusIndex() {
    return 7;
  }

}

public class SnmpTargetAddrTable extends AbstractConceptualTableModel {
  Hashtable index = null;

  public static int OTHER = 1;

  public static int VOLATILE = 2;

  public static int NON_VOLATILE = 3;

  public static int PERMANENT = 4;

  public static int READ_ONLY = 5;

  public OID[] oids;

  public byte[] type;

  public SnmpTargetAddrTable(String mask) {
    super(mask);
    oids = new OID[8];
    type = new byte[8];
    oids[0] = new OID(".1.3.6.1.6.3.12.1.2.1.2"); // No DefVal
    oids[1] = new OID(".1.3.6.1.6.3.12.1.2.1.3"); // No DefVal
    oids[2] = new OID(".1.3.6.1.6.3.12.1.2.1.4"); // 1500
    oids[3] = new OID(".1.3.6.1.6.3.12.1.2.1.5"); // 3
    oids[4] = new OID(".1.3.6.1.6.3.12.1.2.1.6"); // ""
    oids[5] = new OID(".1.3.6.1.6.3.12.1.2.1.7"); // No DefVal
    oids[6] = new OID(".1.3.6.1.6.3.12.1.2.1.8"); // NON_VOLATILE
    oids[7] = new OID(".1.3.6.1.6.3.12.1.2.1.9"); // No DefVal
    type[0] = Var.OID;
    type[1] = Var.OCTETSTRING;
    type[2] = Var.INTEGER;
    type[3] = Var.INTEGER;
    type[4] = Var.OCTETSTRING;
    type[5] = Var.OCTETSTRING;
    type[6] = Var.INTEGER;
    type[7] = Var.INTEGER;

    index = new Hashtable();
  }

  public OID getRowStatusOID() {
    return oids[7];
  }

  public TableProvider createNewProvider(OID key) {
    return new TargetProvider();
  }

  public void index(String tagList, String key) {
    StringTokenizer st = new StringTokenizer(tagList);
    while (st.hasMoreTokens()) {
      index.put(st.nextToken(), key);
    }
  }

  public String getName(String tagValue) {
    return (String) index.get(tagValue);
  }

  public OID[] getColumns() {
    return oids;
  }

  public byte[] getColumnTypes() {
    return type;
  }

  public byte validate(ConceptualTableProvider provider) {
    for (int i = 0; i < oids.length; i++) {
      if (provider.getValueAt(i) == null) {
        return AbstractAgent.NOT_READY;
      }
    }
    return AbstractAgent.NO_ERROR;
  }

  public void setDefaultValues(TableProvider provider) {
    try {
      if (provider.getValueAt(2) == null)
        provider.setValueAt(2, new Int(1500));
      if (provider.getValueAt(3) == null)
        provider.setValueAt(3, new Int(3));
      if (provider.getValueAt(4) == null)
        provider.setValueAt(4, new Str("").toVar());
      if (provider.getValueAt(6) == null)
        provider.setValueAt(6, new Int(NON_VOLATILE));
    } catch (Exception e) {
    }
  }

  public boolean allowChange(int colNumber, TableRow r) {
    try {
      ConceptualTableRow row = (ConceptualTableRow) r;
      if (row.getState() == AbstractAgent.ACTIVE && colNumber == 0)
        return false;
      if (row.getState() == AbstractAgent.ACTIVE && colNumber == 1)
        return false;
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isOIDValid(OID oid) {
    try {
      StringOID o = StringOID.createStringOID("", 0, oid.toString());
    } catch (Exception e) {
      return false;
    }
    return true;
  }

}