import React, { useState, useMemo, useEffect } from 'react';
import './ReportingPage.css'
import axios from 'axios';

const API_BASE = "http://localhost:8080";


const Dashboard = () => {
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [dateRange, setDateRange] = useState('all');
  const [cardTypeFilter, setCardTypeFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');

  const [applications, setApplications] = useState([]);

  const fetchAllApplications = async () => {
        try {
            const token = localStorage.getItem("token"); 
            const response = await axios.get(`${API_BASE}/api/onboarding/all`, {
                headers: { Authorization: `Bearer ${token}` }
            });

            console.log("API data:", response.data);
            setApplications(response.data || []);
        } catch (error) {
            console.error("Error fetching applications:", error.response?.data || error.message);
        }
    };

    useEffect(() => {
        fetchAllApplications();
    }, [ ]);

  const filteredApplications = useMemo(() => {
    return applications.filter(app => {
      const matchesCardType = cardTypeFilter === 'all' || app.cardType === cardTypeFilter;
      const matchesStatus = statusFilter === 'all' || app.status === statusFilter;
      
      let matchesDate = true;
      if (dateRange !== 'all') {
        const appDate = new Date(app.submittedAt);
        const today = new Date('2025-10-13');

        
        if (dateRange === 'last7days') {
          const last7Days = new Date(today);
          last7Days.setDate(today.getDate() - 7);
          matchesDate = appDate >= last7Days;
        } else if (dateRange === 'last30days') {
          const last30Days = new Date(today);
          last30Days.setDate(today.getDate() - 30);
          matchesDate = appDate >= last30Days;
        } else if (dateRange === 'thisMonth') {
          matchesDate = appDate.getMonth() === today.getMonth();
        }
      }
      
      return matchesCardType && matchesStatus && matchesDate;
    });
  }, [dateRange, cardTypeFilter, statusFilter,applications]);

  const stats = useMemo(() => {
    const total = filteredApplications.length;
    const approved = filteredApplications.filter(app => app.status === 'APPROVED').length;
    const pending = filteredApplications.filter(app => app.status === 'PENDING').length;
    const rejected = filteredApplications.filter(app => app.status === 'REJECTED').length;
    
    return { total, approved, pending, rejected };
  }, [filteredApplications]);

  const reportData = useMemo(() => {
    const cardTypes = ['PLATINUM', 'GOLD', 'SILVER'];
    return cardTypes.map(type => {
      const typeApps = filteredApplications.filter(app => app.cardType === type);
      const approved = typeApps.filter(app => app.status === 'APPROVED').length;
      const total = typeApps.length;
      const percentage = total > 0 ? ((approved / total) * 100).toFixed(1) : 0;
      
      return { cardType: type, total, approved, percentage };
    });
  }, [filteredApplications]);

  const resetFilters = () => {
    setDateRange('all');
    setCardTypeFilter('all');
    setStatusFilter('all');
  };

  if (currentPage === 'status') {
    return <StatusPage applications={filteredApplications} onBack={() => setCurrentPage('dashboard')} />;
  }

  if (currentPage === 'report') {
    return <ReportPage reportData={reportData} stats={stats} onBack={() => setCurrentPage('dashboard')} />;
  }

  return (
    <>
   
      <div className="min-vh-100" style={{  padding: '20px' }}>
        <div className="container-fluid">
          <div className="card shadow-sm" style={{  }}>
            <div className="card-body" style={{  }}>
              <h1 className="text-center mb-4 fw-bold gradient-text" >Reporting Dashboard</h1>
              
              <div className="row g-3 mb-4">
                <div className="col-12 col-md-3">
                  <select 
                    className="form-select" 
                    value={dateRange}
                    onChange={(e) => setDateRange(e.target.value)}
                    style={{ background: 'linear-gradient(to bottom, #007b8f)',color:"white", border: '1px solid #000' }}
                  >
                    <option value="" style={{color:"black"}}>Date Range ▼</option>
                    <option value="last7days" style={{color:"black"}}>Last 7 Days</option>
                    <option value="last30days" style={{color:"black"}}>Last 30 Days</option>
                    <option value="thisMonth" style={{color:"black"}}>This Month</option>
                  </select>
                </div>
                
                <div className="col-12 col-md-3">
                  <select 
                    className="form-select"
                    value={cardTypeFilter}
                    onChange={(e) => setCardTypeFilter(e.target.value)}
                    style={{ background: 'linear-gradient(to bottom, #007b8f)',color:"white", border: '1px solid #000' }}
                  >
                    <option value="" style={{color:"black"}}>Card Type ▼</option>
                    <option value="PLATINUM" style={{color:"black"}}>Platinum</option>
                    <option value="GOLD" style={{color:"black"}}>Gold</option>
                    <option value="SILVER" style={{color:"black"}}>Silver</option>
                    
                  </select>
                </div>
                
                <div className="col-12 col-md-3">
                  <button 
                    className="btn w-100"
                    onClick={() => setCurrentPage('status')}
                    style={{ background: 'linear-gradient(to bottom, #007b8f)',color:"white", border: '1px solid #000' }}
                  >
                    Status
                  </button>
                </div>
                
                <div className="col-12 col-md-3">
                  <button 
                    className="btn w-100"
                    onClick={() => setCurrentPage('report')}
                    style={{ background: 'linear-gradient(to bottom, #007b8f)',color:"white", border: '1px solid #000' }}
                  >
                    Generate Report
                  </button>
                </div>
              </div>

              <div className="row mb-4">
                <div className="col-12">
                  <div className="card" style={{ backgroundColor: '', border: '1px solid #000' }}>
                    <div className="card-body">
                      <div className="row text-center">
                        <div className="col-6 col-md-3 mb-3">
                          <h6 className="mb-2">Total Applications</h6>
                          <h2 className="fw-bold">{stats.total}</h2>
                        </div>
                        <div className="col-6 col-md-3 mb-3">
                          <h6 className="mb-2">Approved</h6>
                          <h2 className="fw-bold">{stats.approved}</h2>
                        </div>
                        <div className="col-6 col-md-3">
                          <h6 className="mb-2">Pending</h6>
                          <h2 className="fw-bold">{stats.pending}</h2>
                        </div>
                        <div className="col-6 col-md-3">
                          <h6 className="mb-2">Rejected</h6>
                          <h2 className="fw-bold">{stats.rejected}</h2>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="row mb-4">
                <div className="col-12">
                  <div className="card text-center" style={{ backgroundColor: '', border: '1px solid #000' }}>
                    <div className="card-body py-4">
                      <p className="mb-3 fs-5">
                        As a Banking Organisation that keeps customers first, we are proud to have 
                        achieved a Customer Satisfaction Rate of
                      </p>
                      <h1 className="display-3 fw-bold">{((100/stats.total)*(stats.approved)).toFixed(2)}%</h1>
                    </div>
                  </div>
                </div>
              </div>

              <div className="row" >
                <div className="col-12">
                  <div className="table-responsive" >
                    <table className="table align-middle table-hover" style={{ backgroundColor: '#dbd8e3' }}>
                      <thead style={{ backgroundColor: '#dbd8e3' }}>
                        <tr>
                          <th>Application ID</th>
                          <th>Applicant Name</th>
                          <th>Card Type</th>
                          <th>Status</th>
                          <th>Date</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredApplications.slice(0, 5).map((app) => (
                          <tr key={app.id}>
                            <td>{app.appId.substring(0,8)}</td>
                            <td>{app.fullName}</td>
                            <td>{app.cardType}</td>
                            <td>{app.status}</td>
                            <td>{new Date(app.submittedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>

              {(dateRange !== 'all' || cardTypeFilter !== 'all' || statusFilter !== 'all') && (
                <div className="text-center mt-3">
                  <button className="btn btn-primary" onClick={resetFilters}>
                    Reset Filters
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

const StatusPage = ({ applications, onBack }) => {
  const [statusFilter, setStatusFilter] = useState('all');

  const filteredByStatus = statusFilter === 'all' 
    ? applications 
    : applications.filter(app => app.status === statusFilter);

  const statusCounts = {
    approved: applications.filter(app => app.status === 'APPROVED').length,
    pending: applications.filter(app => app.status === 'IN_PROGRESS').length,
    rejected: applications.filter(app => app.status === 'REJECTED').length,
  };

  return (
    <>
     
      <div className="min-vh-100" style={{ backgroundColor: '', padding: '20px' }}>
        <div className="container-fluid">
          <div className="card shadow-sm" style={{ border: '' }}>
            <div className="card-body" style={{ backgroundColor: '' }}>
              <div className="d-flex justify-content-between align-items-center mb-4">
                <button className="btn btn-secondary" onClick={onBack}>
                  ← Back 
                </button>
                <h1 className="fw-bold mb-0" style={{color:"#007b8f"}}>Application Status</h1>
                <div style={{ width: '100px' }}></div>
              </div>

              <div className="row mb-4">
                <div className="col-12 col-md-4 mb-3">
                  <div className="card text-center" style={{ backgroundColor: 'lightgreen', border: '1px solid #000' }}>
                    <div className="card-body">
                      <h5>Approved</h5>
                      <h2 className="fw-bold">{statusCounts.approved}</h2>
                    </div>
                  </div>
                </div>
                <div className="col-12 col-md-4 mb-3">
                  <div className="card text-center" style={{ backgroundColor: '#ffeb9c', border: '1px solid #000' }}>
                    <div className="card-body">
                      <h5>Pending</h5>
                      <h2 className="fw-bold">{statusCounts.pending}</h2>
                    </div>
                  </div>
                </div>
                <div className="col-12 col-md-4">
                  <div className="card text-center" style={{ backgroundColor: '#da5e5eff', border: '1px solid #000' }}>
                    <div className="card-body">
                      <h5>Rejected</h5>
                      <h2 className="fw-bold">{statusCounts.rejected}</h2>
                    </div>
                  </div>
                </div>
              </div>

              <div className="btn-group w-100 mb-4">
                <button 
                  className={`btn ${statusFilter === 'all' ? 'btn-primary' : 'btn-outline-primary'}`}
                  onClick={() => setStatusFilter('all')}
                >
                  All
                </button>
                <button 
                  className={`btn ${statusFilter === 'APPROVED' ? 'btn-success' : 'btn-outline-success'}`}
                  onClick={() => setStatusFilter('APPROVED')}
                >
                  Approved
                </button>
                <button 
                  className={`btn ${statusFilter === 'IN_PROGRESS' ? 'btn-warning' : 'btn-outline-warning'}`}
                  onClick={() => setStatusFilter('PENDING')}
                >
                  Pending
                </button>
                <button 
                  className={`btn ${statusFilter === 'REJECTED' ? 'btn-danger' : 'btn-outline-danger'}`}
                  onClick={() => setStatusFilter('REJECTED')}
                >
                  Rejected
                </button>
              </div>

              <div className="table-responsive">
                <table className="table align-middle table-hover" style={{ backgroundColor: '#fff' }}>
                  <thead style={{ backgroundColor: '#a8d5ba' }}>
                    <tr>
                      <th>Application ID</th>
                      <th>Applicant Name</th>
                      <th>Card Type</th>
                      <th>Status</th>
                      <th>Application Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredByStatus.map((app) => (
                      <tr key={app.id}>
                        <td>{app.appId.substring(0,8)}</td>
                        <td>{app.fullName}</td>
                        <td>
                          <span className="badge bg-secondary">{app.cardType}</span>
                        </td>
                        <td>
                          <span className={`badge ${
                            app.status === 'APPROVED' ? 'bg-success' : 
                            app.status === 'IN_PROGRESS' ? 'bg-warning text-dark' : 
                            'bg-danger'
                          }`}>
                            {app.status}
                          </span>
                        </td>
                        <td>{new Date(app.submittedAt).toLocaleDateString('en-US', { 
                          year: 'numeric', 
                          month: 'long', 
                          day: 'numeric' 
                        })}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {filteredByStatus.length === 0 && (
                <div className="text-center py-5">
                  <h4 className="text-muted">No applications found with this status</h4>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

const ReportPage = ({ reportData, stats, onBack }) => {
  return (
    <>
  
      <div className="min-vh-100" style={{ backgroundColor: '', padding: '20px' }}>
        <div className="container-fluid">
          <div className="card shadow-sm" style={{ border: '' }}>
            <div className="card-body" style={{ backgroundColor: '' }}>
              <div className="d-flex justify-content-between align-items-center mb-4">
                <button className="btn btn-secondary" onClick={onBack}>
                  ← Back 
                </button>
                <h1 className="fw-bold mb-0" style={{color:"#007b8f"}}>Approval Report</h1>
                <div style={{ width: '100px' }}></div>
              </div>

              <div className="row mb-4">
                <div className="col-12">
                  <div className="card" style={{ backgroundColor: '', border: '1px solid #000' }}>
                    <div className="card-body">
                      <h4 className="text-center mb-3">Overall Statistics</h4>
                      <div className="row text-center">
                        <div className="col-6 col-md-3">
                          <h6>Total Applications</h6>
                          <h3 className="fw-bold">{stats.total}</h3>
                        </div>
                        <div className="col-6 col-md-3">
                          <h6>Approved</h6>
                          <h3 className="fw-bold text-success">{stats.approved}</h3>
                        </div>
                        <div className="col-6 col-md-3">
                          <h6>Pending</h6>
                          <h3 className="fw-bold text-warning">{stats.pending}</h3>
                        </div>
                        <div className="col-6 col-md-3">
                          <h6>Rejected</h6>
                          <h3 className="fw-bold text-danger">{stats.rejected}</h3>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="row mb-4">
                <div className="col-12">
                  <h4 className="mb-3">Approval Rate by Card Type</h4>
                  <div className="table-responsive">
                    <table className="table align-middle table-hover" style={{ backgroundColor: '#fff' }}>
                      <thead style={{ backgroundColor: '#dbd8e3' }}>
                        <tr>
                          <th>Card Type</th>
                          <th>Total Applications</th>
                          <th>Approved Applications</th>
                          <th>Approval Rate</th>
                          <th>Visual Progress</th>
                        </tr>
                      </thead>
                      <tbody>
                        {reportData.map((data) => (
                          <tr key={data.cardType}>
                            <td className="fw-bold">{data.cardType}</td>
                            <td>{data.total}</td>
                            <td>{data.approved}</td>
                            <td>
                              <span className={`badge fs-6 ${
                                parseFloat(data.percentage) >= 70 ? 'bg-success' : 
                                parseFloat(data.percentage) >= 50 ? 'bg-warning text-dark' : 
                                'bg-danger'
                              }`}>
                                {data.percentage}%
                              </span>
                            </td>
                            <td>
                              <div className="progress" style={{ height: '25px' }}>
                                <div 
                                  className={`progress-bar ${
                                    parseFloat(data.percentage) >= 70 ? 'bg-success' : 
                                    parseFloat(data.percentage) >= 50 ? 'bg-warning' : 
                                    'bg-danger'
                                  }`}
                                  style={{ width: `${data.percentage}%` }}
                                >
                                  {data.percentage}%
                                </div>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>

              <div className="row">
                {reportData.map((data) => (
                  <div key={data.cardType} className="col-12 col-md-6 col-lg-4 mb-3">
                    <div className="card h-100" style={{ 
                      backgroundColor: '', 
                      border: '2px solid #000',
                      borderRadius: '10px'
                    }}>
                      <div className="card-body text-center">
                        <h5 className="fw-bold">{data.cardType}</h5>
                        <div className="my-3">
                          <div style={{ 
                            width: '100px', 
                            height: '100px', 
                            margin: '0 auto',
                            borderRadius: '50%',
                            background: `conic-gradient(#28a745 ${data.percentage}%, #e0e0e0 0)`,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                          }}>
                            <div style={{
                              width: '80px',
                              height: '80px',
                              borderRadius: '50%',
                              backgroundColor: '#fff',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              fontWeight: 'bold',
                              fontSize: '1.2rem'
                            }}>
                              {data.percentage}%
                            </div>
                          </div>
                        </div>
                        <p className="mb-1"><strong>Total:</strong> {data.total}</p>
                        <p className="mb-0"><strong>Approved:</strong> {data.approved}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              <div className="text-center mt-4">
                <button className="btn btn-primary btn-lg" onClick={() => window.print()}>
                  Print Report
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default Dashboard