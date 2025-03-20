package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.basic.Angle;
import eu.printingin3d.javascad.basic.Radius;
import eu.printingin3d.javascad.context.IColorGenerationContext;
import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.coords.Boundary;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.coords2d.Boundaries2d;
import eu.printingin3d.javascad.coords2d.Coords2d;
import eu.printingin3d.javascad.coords2d.LineSegment2d;
import eu.printingin3d.javascad.enums.PointRelation;
import eu.printingin3d.javascad.models2d.Abstract2dModel;
import eu.printingin3d.javascad.models2d.Area2d;
import eu.printingin3d.javascad.utils.Color;
import eu.printingin3d.javascad.utils.DoubleUtils;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import eu.printingin3d.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Linear extrude the given 2D model to create a 3D object.
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class LinearExtrude extends Atomic3dModel {
	private final Abstract2dModel model;
	private final double height;
	private final Angle twist;
	private final double scale;

	/**
	 * Constructs a 3D object based on the given parameters.
	 * @param model the 2D model to be extruded
	 * @param height the length of the extrusion. That will be the height of the resulted 3D model
	 */
	public LinearExtrude(Abstract2dModel model, double height) {
		this.model = model;
		this.height = height;
		this.twist = Angle.ZERO;
		this.scale = 1;
	}

	/**
	 * Constructs a 3D object based on the given parameters.
	 * @param model the 2D model to be extruded
	 * @param height the length of the extrusion. That will be the height of the resulted 3D model
	 * @param twist the rotation of the 2D model during the extrusion in degrees
	 * @param scale the scaling of the 2D model during the extrusion. 1.0 means no change.
	 */
	public LinearExtrude(Abstract2dModel model, double height, Angle twist, double scale) {
		this.model = model;
		this.height = height;
		this.twist = twist;
		this.scale = scale;
	}
	
	/**
	 * Constructs a 3D object based on the given parameters. The result is exactly the same as if you
	 * call <code>new LinearExtrude(model, height, twist, 1.0)</code>.
	 * @param model the 2D model to be extruded
	 * @param height the length of the extrusion. That will be the height of the resulted 3D model
	 * @param twist the rotation of the 2D model during the extrusion in degrees
	 */
	public LinearExtrude(Abstract2dModel model, double height, Angle twist) {
		this(model, height, twist, 1.0);
	}

	@Override
	protected Abstract3dModel innerCloneModel() {
		return new LinearExtrude(model, height, twist, scale);
	}

	@Override
	protected SCAD innerToScad(IColorGenerationContext context) {
		return new SCAD("linear_extrude(height="+DoubleUtils.formatDouble(height)
					+ ", center=true, convexity=10, "
					+ "twist="+twist
					+ ",scale="+DoubleUtils.formatDouble(scale)+")")
				.append(model.toScad(context));
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		Boundaries2d boundaries2d = model.getBoundaries2d();
		Boundary boundaryX;
		Boundary boundaryY;
		if (twist.isZero()) {
			boundaryX = boundaries2d.getX();
			boundaryY = boundaries2d.getY();
		}
		else {
			boundaryX = new Boundary(
					boundaries2d.getX().getMax(), -boundaries2d.getX().getMax(), 
					boundaries2d.getX().getMin(), -boundaries2d.getX().getMin(),
					boundaries2d.getY().getMax(), -boundaries2d.getY().getMax(),
					boundaries2d.getY().getMin(), -boundaries2d.getY().getMin());
			boundaryY = boundaryX;
		}
		
		return new Boundaries3d(
					boundaryX,
					boundaryY,
					Boundary.createSymmetricBoundary(height/2.0)
				);
	}
	
	private static List<Area2d> generateCover(Area2d area) {
		List<Area2d> result = new ArrayList<>();
		Area2d coords = area;
	
		for (int i=0;i<area.size();i++) {
			if (coords.size()<=2) {
				break;
			}
/*		while (coords.size()>2) {
			if (coords.size()==3) {
				result.add(coords);
				break;
			}*/
			Coords2d p = coords.get(0);
			Coords2d prev = p;
			int count = 0;
			for (Coords2d c : coords) {
				count++;
				if (c!=p && 
						(!area.findCrossing(new LineSegment2d(c, p), false).isEmpty() || 
							area.calculatePointRelation(Coords2d.midPoint(c, p))==PointRelation.OUTSIDE)) {
					break;
				}
				prev = c;
			}
			
			if (count>3) {
				result.add(coords.subList(p, prev));
				coords = coords.subList(prev, p);
			} else {
				coords = coords.subList(1, 0);
			}
		}
		return result;
	}

	@Override
	protected CSG toInnerCSG(FacetGenerationContext context) {
		// Получаем 2D-области из проекции
		Collection<Area2d> areas = model.getPointCircle(context);

		// Экструдируем каждую область в 3D
		List<Polygon> polygons = new ArrayList<>();
		for (Area2d area : areas) {
			polygons.addAll(extrudeArea(area, height));
		}

		return new CSG(polygons);
	}

	//@Override
	protected CSG toInnerCSG1(FacetGenerationContext context) {
		Color color = context.getColor();
		List<Polygon> polygons = new ArrayList<>();
		
		for (Area2d points : model.getPointCircle(context)) {
			int numOfSteps = twist.isZero() ? 1 : context.calculateNumberOfSlices(Radius.fromRadius(height));
	
			double y1=-height/2;
			Angle alpha1 = Angle.ZERO;
			List<V3d> c1 = points.rotate(alpha1).withZ(y1);
	
			for (Area2d lc : generateCover(points.rotate(alpha1).reverse())) {
				polygons.add(Polygon.fromPolygons(lc.withZ(y1), color));
			}
			
			for (int i=1;i<=numOfSteps;i++) {
				double y2 = i*height/numOfSteps-height/2;
				Angle alpha2 = twist.mul(i).divide(numOfSteps);
				
				List<V3d> c2 = points.rotate(alpha2).withZ(y2);
				
				for (int t=0;t<c1.size();t++) {
					int p = (t+1) % c1.size();
					
					polygons.add(Polygon.fromPolygons(Arrays.asList(
							c1.get(t),
							c2.get(p),
							c2.get(t)
						), color));
					polygons.add(Polygon.fromPolygons(Arrays.asList(
							c1.get(t),
							c1.get(p),
							c2.get(p)
							), color));
				}
				
				c1 = c2;
				y1 = y2;
				alpha1 = alpha2;
			}
			for (Area2d lc : generateCover(points.rotate(alpha1))) {
				polygons.add(Polygon.fromPolygons(lc.withZ(y1), color));
			}
		}
		
		return new CSG(polygons);
	}

	private List<Polygon> extrudeArea(Area2d area, double height) {
		List<Polygon> polygons = new ArrayList<>();
		List<Coords2d> points = area.getPoints();
		int n = points.size();

		// Создаем верхнюю и нижнюю грани
		List<V3d> topFace = points.stream()
			.map(p -> p.withZ(height))
			.collect(Collectors.toList());
		List<V3d> bottomFace = points.stream()
			.map(p -> p.withZ(0))
			.collect(Collectors.toList());

		polygons.add(Polygon.fromPolygons(topFace, Color.RED));    // Верхняя грань
		polygons.add(Polygon.fromPolygons(bottomFace, Color.BLUE)); // Нижняя грань

		// Создаем боковые грани
		for (int i = 0; i < n; i++) {
			Coords2d current = points.get(i);
			Coords2d next = points.get((i + 1) % n);

			List<V3d> side = Arrays.asList(
				current.withZ(0),
				next.withZ(0),
				next.withZ(height),
				current.withZ(height)
			);

			polygons.add(Polygon.fromPolygons(side, Color.GREEN)); // Боковая грань
		}

		return polygons;
	}
}
