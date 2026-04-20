import { Box, Card, CardContent, Grid, Typography, Skeleton } from '@mui/material';
import {
  People as PeopleIcon,
  Flight as FlightIcon,
  Payment as PaymentIcon,
  TrendingUp as TrendingUpIcon,
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { apiClient } from '../api/client';

interface StatsResponse {
  totalUsers: number;
  totalTravels: number;
  totalPayments: number;
  totalRevenue: number;
  recentPayments: Array<{
    date: string;
    amount: number;
  }>;
  travelsByStatus: Array<{
    status: string;
    count: number;
  }>;
}

const COLORS = ['#1976d2', '#2e7d32', '#ed6c02', '#9c27b0', '#d32f2f'];

function StatCard({
  title,
  value,
  icon,
  color,
  loading,
}: {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
  loading?: boolean;
}) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              {title}
            </Typography>
            {loading ? (
              <Skeleton width={80} height={40} />
            ) : (
              <Typography variant="h4" fontWeight="bold">
                {value}
              </Typography>
            )}
          </Box>
          <Box
            sx={{
              p: 1.5,
              borderRadius: 2,
              backgroundColor: `${color}15`,
              color: color,
            }}
          >
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
}

export default function Dashboard() {
  const { data: stats, isLoading } = useQuery<StatsResponse>({
    queryKey: ['dashboard-stats'],
    queryFn: async () => {
      // This would call a real stats endpoint
      // For now, return mock data
      return {
        totalUsers: 1250,
        totalTravels: 438,
        totalPayments: 892,
        totalRevenue: 125890,
        recentPayments: [
          { date: 'Jan', amount: 12500 },
          { date: 'Fév', amount: 18200 },
          { date: 'Mar', amount: 15800 },
          { date: 'Avr', amount: 22100 },
          { date: 'Mai', amount: 19500 },
          { date: 'Juin', amount: 25890 },
        ],
        travelsByStatus: [
          { status: 'En cours', count: 85 },
          { status: 'Planifié', count: 120 },
          { status: 'Terminé', count: 198 },
          { status: 'Annulé', count: 35 },
        ],
      };
    },
  });

  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Tableau de bord
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Vue d'ensemble de votre plateforme Travel Plan
      </Typography>

      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Utilisateurs"
            value={stats?.totalUsers.toLocaleString() || 0}
            icon={<PeopleIcon fontSize="large" />}
            color="#1976d2"
            loading={isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Voyages"
            value={stats?.totalTravels.toLocaleString() || 0}
            icon={<FlightIcon fontSize="large" />}
            color="#2e7d32"
            loading={isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Paiements"
            value={stats?.totalPayments.toLocaleString() || 0}
            icon={<PaymentIcon fontSize="large" />}
            color="#ed6c02"
            loading={isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Revenus"
            value={`${(stats?.totalRevenue || 0).toLocaleString()} €`}
            icon={<TrendingUpIcon fontSize="large" />}
            color="#9c27b0"
            loading={isLoading}
          />
        </Grid>

        {/* Revenue Chart */}
        <Grid item xs={12} md={8}>
          <Card sx={{ height: 400 }}>
            <CardContent>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Revenus mensuels
              </Typography>
              {isLoading ? (
                <Skeleton variant="rectangular" height={300} />
              ) : (
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={stats?.recentPayments}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" />
                    <YAxis />
                    <Tooltip
                      formatter={(value: number) => [`${value.toLocaleString()} €`, 'Montant']}
                    />
                    <Line
                      type="monotone"
                      dataKey="amount"
                      stroke="#1976d2"
                      strokeWidth={3}
                      dot={{ fill: '#1976d2', strokeWidth: 2, r: 4 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Travel Status Distribution */}
        <Grid item xs={12} md={4}>
          <Card sx={{ height: 400 }}>
            <CardContent>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Voyages par statut
              </Typography>
              {isLoading ? (
                <Skeleton variant="rectangular" height={300} />
              ) : (
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={stats?.travelsByStatus}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={100}
                      dataKey="count"
                      nameKey="status"
                      label={({ status, percent }) =>
                        `${status} ${(percent * 100).toFixed(0)}%`
                      }
                    >
                      {stats?.travelsByStatus.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
