package de.mpicbg.ms.model.data;

import de.mpicbg.ms.view.treecell.CheckBoxNamedBoolean;
import de.mpicbg.ms.view.treecell.NamedBoolean;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;

/**
 * Created by moon on 5/17/16.
 */
public class FAAnionRow
{
   NamedBoolean title;
   String name;

   Double mass;
   CO2 co2mass;

   String co2name;

   NamedBoolean sn1, sn2, sym;

   NamedBoolean co2MassCheckBox;

   int index;
   int faAnionIndex;

   Float compQuant;
   SimpleFloatProperty isomer;

   SimpleBooleanProperty validProperty;

   Boolean master = false;

   String clazz;

   public FAAnionRow( FAAnion faAnion )
   {
      this.mass = faAnion.getMass();
      this.title = new CheckBoxNamedBoolean( this.name );
      this.sn1 = new CheckBoxNamedBoolean( "0" );
      this.sn2 = new CheckBoxNamedBoolean( "0" );
      this.sym = new CheckBoxNamedBoolean( "0" );

      this.co2MassCheckBox = new NamedBoolean( "" );

      this.faAnionIndex = faAnion.index;
      this.isomer = new SimpleFloatProperty( faAnion.getFAIsomer() );
      this.validProperty = new SimpleBooleanProperty( false );
   }

   public FAAnionRow( int index, FAAnion faAnion )
   {
      this( faAnion );
      this.index = index;
      this.name = index + "";
   }

   public FAAnionRow( String name, FAAnion faAnion )
   {
      this( faAnion );
      this.name = name;
   }

   public FAAnionRow( int index, FAAnion faAnion, String clazz )
   {
      this( index, faAnion );
      this.clazz = clazz;
   }

   public FAAnionRow( int index, Double mass, Float iso )
   {
      this.index = index;
      this.name = index + "";

      this.mass = mass;
      this.title = new CheckBoxNamedBoolean( this.name );
      this.sn1 = new CheckBoxNamedBoolean( "0" );
      this.sn2 = new CheckBoxNamedBoolean( "0" );
      this.sym = new CheckBoxNamedBoolean( "0" );

      this.co2MassCheckBox = new NamedBoolean( "" );
      this.isomer = new SimpleFloatProperty( iso );
      this.validProperty = new SimpleBooleanProperty( false );
   }

   public FAAnionRow( String name )
   {
      this.name = name;
      this.title = new NamedBoolean( this.name );

      this.sn1 = new NamedBoolean( "0" );
      this.sn2 = new NamedBoolean( "0" );
      this.sym = new NamedBoolean( "0" );

      this.co2MassCheckBox = new NamedBoolean( "" );
      this.validProperty = new SimpleBooleanProperty( false );
   }

   public FAAnionRow( String name, String className )
   {
      this( name );
      this.clazz = className;
   }

   @Override public boolean equals( Object obj )
   {
      return name.equals( obj );
   }

   @Override public int hashCode()
   {
      return name.hashCode();
   }

   @Override public String toString()
   {
      return name;
   }

   public Double getMass()
   {
      return mass;
   }

   public CO2 getCo2Mass()
   {
      return co2mass;
   }

   public String getMassString()
   {
      if ( mass == null )
         return "";
      return mass.toString();
   }

   public String getCo2MassString()
   {
      return co2MassCheckBox.getName();
   }

   public void setCo2MassString( String co2MassString )
   {
      co2MassCheckBox.setName( co2MassString );
   }

   public void setCo2mass( CO2 co2mass )
   {
      this.co2mass = co2mass;

      if ( co2MassCheckBox instanceof NamedBoolean )
      {
         co2MassCheckBox = new CheckBoxNamedBoolean( co2mass.getMass().toString() );
      }
   }

   public void setCo2mass()
   {
      if ( co2MassCheckBox instanceof NamedBoolean )
      {
         co2MassCheckBox = new CheckBoxNamedBoolean( co2MassCheckBox.getName() );
      }
   }

   public String getCo2name()
   {
      return co2name;
   }

   public void setCo2name( String co2name )
   {
      this.co2name = co2name;
   }

   public String getName()
   {
      return name;
   }

   public NamedBoolean getSn1()
   {
      return sn1;
   }

   public NamedBoolean getSn2()
   {
      return sn2;
   }

   public NamedBoolean getSym()
   {
      return sym;
   }

   public NamedBoolean getCo2MassProperty()
   {
      return co2MassCheckBox;
   }

   public NamedBoolean getTitle()
   {
      return title;
   }

   public void newTitle( NamedBoolean old )
   {
      this.title = new CheckBoxNamedBoolean( this.name );
      this.title.setValue( old.getValue() );
   }

   public void newCo2Mass( NamedBoolean old )
   {
      this.co2MassCheckBox = new CheckBoxNamedBoolean( old.getName() );
      this.co2MassCheckBox.setValue( old.getValue() );
   }

   public Float getCompQuant()
   {
      return compQuant;
   }

   public void setCompQuant( Float compQuant )
   {
      this.compQuant = compQuant;
   }

   public String getCQString()
   {
      if ( compQuant == null )
         return "";
      else
         return compQuant.toString();
   }

   public String getIsomer()
   {
      if ( isomer == null )
         return "";
      else
         return isomer.getValue().toString();
   }

   public Float getIsomerValue()
   {
      return isomer.getValue();
   }

   public SimpleFloatProperty isomerProperty()
   {
      return isomer;
   }

   public void setIsomer( Float isomer )
   {
      this.isomer.set( isomer );
   }

   public boolean getValidProperty()
   {
      return validProperty.get();
   }

   public SimpleBooleanProperty validProperty()
   {
      return validProperty;
   }

   public boolean isMaster()
   {
      return master;
   }

   public void setMaster( boolean master )
   {
      this.master = master;
   }

   public String getClazz()
   {
      return clazz;
   }

   public void setClazz( String clazz )
   {
      this.clazz = clazz;
   }
}
