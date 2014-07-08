/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */

package com.mortennobel.imagescaling;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

/**
 * May have to adjust heap using the Xmx vm option
 */
public class SpeedSingleThreadTest extends TestCase {

	protected BufferedImage image;

	public int getNumberOfThreads(){
		return 1;
	}

	@Override
    protected void setUp() throws Exception {
		super.setUp();
		this.image = ImageIO.read(getClass().getResource("largeimage.jpg"));
	}

	protected void doRescale(final ResampleFilter filter) throws Exception{
		final ResampleOp resampleOp = new ResampleOp(200,200);
		resampleOp.setNumberOfThreads(getNumberOfThreads());
		resampleOp.setFilter(filter);
		resampleOp.filter(this.image, null);
	}

	public void testBellFilter() throws Exception {
		doRescale(new BellFilter());
	}

	public void testBiCubicFilter() throws Exception {
		doRescale(new BiCubicFilter());
	}

	public void testBiCubicHighFreqResponse() throws Exception {
		doRescale(new BiCubicHighFreqResponse());
	}

	public void testBoxFilter() throws Exception {
		doRescale(new BoxFilter());
	}

	public void testBSplineFilter() throws Exception {
		doRescale(new BSplineFilter());
	}

	public void testHermiteFilter() throws Exception {
		doRescale(new HermiteFilter());
	}

	public void testLanczos3Filter() throws Exception {
		doRescale(new Lanczos3Filter());
	}

	public void testMitchellFilter() throws Exception {
		doRescale(new MitchellFilter());
	}

	public void testTriangleFilter() throws Exception {
		doRescale(new TriangleFilter());
	}

	public void testMultiStepRescaleOpLinear(){
		final MultiStepRescaleOp mro = new MultiStepRescaleOp(200,200, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		mro.filter(this.image, null);
	}

	public void testMultiStepRescaleOpBicubic(){
		final MultiStepRescaleOp mro = new MultiStepRescaleOp(200,200, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		mro.filter(this.image, null);
	}
}
