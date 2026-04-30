# Generate multiindices of different degree on each dimension.
# Useful when the available data does not have same resolution
# along different dimensions.
#from minterpy.jit_compiled_utils import lp_norm_for_exponents  # This does not exist anymore

# basic python packages
import numpy as np
import pandas as pd

# specific python packages
import minterpy as mp
import cvxpy as cp
import sympy as sp # added by Damar for handling equations

# visualisation tools
from matplotlib.widgets  import RectangleSelector
from minterpy.core.utils import get_exponent_matrix

####

def poly_evaluate_at(n_poly, eval_points, lower_bounds, upper_bounds):
    N_points, spatial_dim = eval_points.shape

    result = np.zeros(N_points)
    for p in range(N_points):
        coord = eval_points[p,:]
        resized_coord = (np.divide(coord - lower_bounds, upper_bounds - lower_bounds) * 2.0) - 1.0
        
        result[p] = n_poly(resized_coord)[0]

    return result

def gen_multi_indices(spatial_dimension, degree_along_dim, lp_degree):
    max_exponent = np.max(degree_along_dim)
    exponents = get_exponent_matrix(spatial_dimension, max_exponent, lp_degree)
    N, m = exponents.shape

    # initial filtering step
    mask = [True] * N
    for i in range(m):
        dim_mask = exponents[:,i] <= degree_along_dim[i]
        mask = mask & dim_mask

    reduced_exp = exponents[mask,:]

    return reduced_exp

########

def lasso_fit_data(
    sample_points,
    function_vals,
    poly_degree_along,
    weighted=True,
):
    """ Regression with L1 fitting for mass spec data.

    Note: function_vals has to be provided in actual scale as we take the log2 of it here.
    
    Parameters
    ----------
    sample_points : coordinates of data
    function_vals : value at the coordinates (eg. intensity)
    poly_degree_along : array specifying the poly degree along each data coordinate
    weighted : weighting with function value enabled if True

    Returns
    -------
    A polynomial (in Newton basis)
    """

    spatial_dim = sample_points.shape[1]

    if len(poly_degree_along) != spatial_dim:
        raise ValueError(f"Poly degrees not correctly specified. \
                            Expected array with {spatial_dim} entries. Got {len(poly_degree_along)}.")

    custom_exponents = gen_multi_indices(spatial_dim, poly_degree_along, 2.0)

    coord = np.array(sample_points, dtype=np.float_)

    lower_bounds = np.round(np.amin(coord, axis=0))
    upper_bounds = np.round(np.amax(coord, axis=0))+1.0

    # Convert coordinates to within the [-1,1] domain
    resized_coord = (np.divide(coord - lower_bounds, upper_bounds - lower_bounds) * 2.0) - 1.0

    ## The log of the intensities
    F = np.log2(function_vals)

    # Create unisolvent nodes with the custom set of exponents
    mi = mp.MultiIndexSet(custom_exponents, lp_degree=2.0)  # NOTE: lp_degree must now be specified
    grid = mp.Grid(mi)
    regressor = mp.OrdinaryRegression(grid=grid)

    # NOTE: cache_transform has been deprecated, below is a work around
    # regressor.cache_transform(resized_coord)  # this does not exist anymore
    rr = regressor.get_regression_matrix(resized_coord)
    nr_data_samples, m = resized_coord.shape
    nr_coeffs = len(regressor.multi_index)
    rr = rr.reshape(nr_data_samples, nr_coeffs)
    
    R = rr # regressor.regression_matrix
    # Access public property instead
    # l2c = mp.transformations.LagrangeToCanonical(regressor._lagrange_poly).transformation_operator
    l2c = mp.transformations.LagrangeToCanonical(regressor.origin_poly).transformation_operator

    # Create two scalar optimization variables.
    x = cp.Variable(R.shape[1])

    if weighted:
        wmat = np.diag(np.sqrt(function_vals))
    else:
        wmat = np.eye(R.shape[0])

    Rmat = wmat @ R
    Fmat = F @ wmat
    # Form objective.
    loss1 = cp.sum_squares(Rmat @ x - Fmat)
    loss2 = cp.sum(cp.abs(l2c @ x))
    obj = cp.Minimize(loss1 + loss2)

    # Form and solve problem.
    prob = cp.Problem(obj)
    prob.solve()

    print(f"Optimizer result : {prob.value}")

    sparse_lag_poly = mp.LagrangePolynomial.from_poly(regressor.origin_poly, new_coeffs=x.value)
    l2n = mp.get_transformation(sparse_lag_poly, mp.NewtonPolynomial)
    sparse_poly_model = l2n()

    return sparse_poly_model

########

# Definitions for handling equations

def find_match_positions(
    larger_idx_set: np.ndarray,
    smaller_idx_set: np.ndarray,
) -> np.ndarray:
    """Find the matching positions of multi-indices in a larger set.

    Parameters
    ----------
    larger_idx_set : np.ndarray
        The set of indices in which to search.
    smaller_idx_set : np.ndarray
        The set of indices to search (i.e., whose position to match).

    Returns
    -------
    np.ndarray
        The positions of the indices.
    """
    nr_exp_smaller, spatial_dimension = smaller_idx_set.shape
    positions = np.zeros(nr_exp_smaller, dtype=np.int64)
    for i in range(nr_exp_smaller):
        idx1 = smaller_idx_set[i, :]
        search_pos = -1
        while True:
            search_pos += 1
            idx2 = larger_idx_set[search_pos, :]
            if is_equal(idx1, idx2):
                positions[i] = search_pos
                break
  
    return positions

def is_equal(index1: np.ndarray, index2: np.ndarray) -> bool:
    """Check the equality between two arrays of multi-indices.
    
    Parameters
    ----------
    index1 : np.ndarray
        First operand
    index2 : np.ndarray
        Second operand

    Returns
    -------
    bool
        True if the arrays are equal in value; False otherwise.
    """
    spatial_dimension = len(index1)
    for m in range(spatial_dimension - 1, -1, -1):  # from last to first dimension
        if index1[m] > index2[m]:
            return False
        if index1[m] < index2[m]:
            return False

    return True  # all equal

def sympy_to_mp(poly, lp_degree: float = 2.0, target_basis=None):
    """Convert a SymPy Poly object to a minterpy poly. in the target basis.
    
    Parameters
    ----------
    poly : sympy.polys.polytools.Poly
        SymPy Poly object.
    lp_degree : float, optional
        The lp-degree of the Minterpy polynomial.
    target_type : mp.core.ABC.MultivariatePolynomialSingleABC, optional
        The target Minterpy polynomial basis;
        the default is the canonical basis.
    """
    given_coeffs = poly.coeffs()
    monoms = np.array(poly.monoms())

    poly_deg = mp.core.utils._get_poly_degree(monoms, lp_degree=2.0)
    dim = monoms.shape[-1]
    mi = mp.MultiIndexSet.from_degree(
        spatial_dimension=dim,
        poly_degree=poly_deg,
    )
    complete_monoms = mi.exponents

    nr_coeffs, _ = complete_monoms.shape
    pos = find_match_positions(complete_monoms, monoms)

    coeffs = np.zeros(nr_coeffs)
    for i in range(len(given_coeffs)):
        coeffs[pos[i]] = given_coeffs[i]

    # By default return Canonical polynomial
    can_poly = mp.CanonicalPolynomial(mi, coeffs)
    if target_basis is None:
        return can_poly
    else:
        transform_to_target = mp.get_transformation(can_poly, target_basis)
        res_poly = transform_to_target()

        return res_poly

def mp_to_sympy(mp_poly):
    """Convert a Minterpy poly. object to a SymPy Poly object.

    Parameters
    ----------
    mp_poly : mp.core.ABC.MultivariatePolynomialSingleABC
        A Minterpy polynomial object (in any basis)

    Returns
    -------
    sympy.polys.polytools.Poly
        SymPy Poly object.
    """

    # Convert to a Canonical basis
    poly2can = mp.get_transformation(mp_poly, mp.CanonicalPolynomial)
    can_poly = poly2can()

    # Make symbol list
    num_coeffs = len(can_poly.coeffs)
    symbol_list = ""
    for i in range(can_poly.spatial_dimension):
        symbol_list += f"x_{i} "

    gen_list = sp.symbols(symbol_list)

    # Construct poly dict
    poly_dict = {
        tuple(can_poly.multi_index.exponents[p]): can_poly.coeffs[p] for p in range(num_coeffs)
    }
    poly = sp.Poly.from_dict(poly_dict, gen_list)

    return poly