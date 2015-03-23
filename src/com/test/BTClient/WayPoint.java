package com.test.BTClient;

public class WayPoint {
	String wpName;
	int latitude,longtitude;//Î³¶È¡¢¾­¶ÈP
	 
	public WayPoint(String wp)
	{
		wpName=wp;
	}
	public WayPoint(String wp,int lat,int lon)
	{
		wpName=wp;
		latitude=lat;
		longtitude=lon;
	}
	public void setWP(String wp,int lat,int lon)
	{
		wpName=wp;
		latitude=lat;
		longtitude=lon;
	}
	public void setPoint( int lat,int lon)
	{ 
		latitude=lat;
		longtitude=lon;
	}
	public String getWPName()
	{
		return wpName;
	}
	public int getLat()
	{
		return latitude;
	}
	public int getLon()
	{
		return longtitude;
	}
}
