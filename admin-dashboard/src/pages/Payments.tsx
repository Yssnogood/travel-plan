import { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Typography,
  IconButton,
  Chip,
  MenuItem,
  Alert,
  Snackbar,
} from '@mui/material';
import { DataGrid, GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import {
  Add as AddIcon,
  Visibility as ViewIcon,
  Replay as RefundIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { paymentService, Payment, CreatePaymentRequest } from '../api/paymentService';

const STATUS_OPTIONS = [
  { value: 'PENDING', label: 'En attente', color: 'warning' },
  { value: 'PROCESSING', label: 'En cours', color: 'info' },
  { value: 'COMPLETED', label: 'Complété', color: 'success' },
  { value: 'FAILED', label: 'Échoué', color: 'error' },
  { value: 'REFUNDED', label: 'Remboursé', color: 'default' },
  { value: 'CANCELLED', label: 'Annulé', color: 'default' },
];

export default function Payments() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [refundDialogOpen, setRefundDialogOpen] = useState(false);
  const [selectedPayment, setSelectedPayment] = useState<Payment | null>(null);
  const [refundAmount, setRefundAmount] = useState<number>(0);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<CreatePaymentRequest>();

  const { data: paymentsData, isLoading } = useQuery({
    queryKey: ['payments', page, pageSize, statusFilter],
    queryFn: () => paymentService.getAllPayments({ page, size: pageSize, status: statusFilter || undefined }),
  });

  const createMutation = useMutation({
    mutationFn: (data: CreatePaymentRequest) => paymentService.createPayment(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      setDialogOpen(false);
      reset();
      setSnackbar({ open: true, message: 'Paiement créé avec succès', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Erreur lors de la création', severity: 'error' });
    },
  });

  const processMutation = useMutation({
    mutationFn: (id: number) => paymentService.processPayment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      setSnackbar({ open: true, message: 'Paiement traité', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Erreur lors du traitement', severity: 'error' });
    },
  });

  const refundMutation = useMutation({
    mutationFn: ({ id, amount }: { id: number; amount?: number }) => paymentService.refundPayment(id, amount),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      setRefundDialogOpen(false);
      setSelectedPayment(null);
      setSnackbar({ open: true, message: 'Remboursement effectué', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Erreur lors du remboursement', severity: 'error' });
    },
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => paymentService.cancelPayment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      setSnackbar({ open: true, message: 'Paiement annulé', severity: 'success' });
    },
    onError: () => {
      setSnackbar({ open: true, message: 'Erreur lors de l\'annulation', severity: 'error' });
    },
  });

  const getStatusConfig = (status: string) => {
    return STATUS_OPTIONS.find((s) => s.value === status) || STATUS_OPTIONS[0];
  };

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'transactionId', headerName: 'Transaction', width: 150 },
    { field: 'userId', headerName: 'User ID', width: 90 },
    { field: 'travelId', headerName: 'Travel ID', width: 90 },
    {
      field: 'amount',
      headerName: 'Montant',
      width: 120,
      valueFormatter: (value: number, row: Payment) => `${value.toLocaleString()} ${row.currency}`,
    },
    {
      field: 'status',
      headerName: 'Statut',
      width: 130,
      renderCell: (params: GridRenderCellParams) => {
        const config = getStatusConfig(params.value);
        return (
          <Chip
            label={config.label}
            color={config.color as any}
            size="small"
          />
        );
      },
    },
    { field: 'description', headerName: 'Description', width: 180 },
    {
      field: 'createdAt',
      headerName: 'Date',
      width: 150,
      valueFormatter: (value: string) => new Date(value).toLocaleString('fr-FR'),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      sortable: false,
      renderCell: (params: GridRenderCellParams<Payment>) => (
        <Box>
          <IconButton size="small">
            <ViewIcon fontSize="small" />
          </IconButton>
          {params.row.status === 'PENDING' && (
            <IconButton
              size="small"
              color="primary"
              onClick={() => processMutation.mutate(params.row.id)}
            >
              <Chip label="Traiter" size="small" color="primary" />
            </IconButton>
          )}
          {params.row.status === 'COMPLETED' && (
            <IconButton
              size="small"
              color="warning"
              onClick={() => {
                setSelectedPayment(params.row);
                setRefundAmount(params.row.amount);
                setRefundDialogOpen(true);
              }}
            >
              <RefundIcon fontSize="small" />
            </IconButton>
          )}
          {(params.row.status === 'PENDING' || params.row.status === 'PROCESSING') && (
            <IconButton
              size="small"
              color="error"
              onClick={() => cancelMutation.mutate(params.row.id)}
            >
              <CancelIcon fontSize="small" />
            </IconButton>
          )}
        </Box>
      ),
    },
  ];

  const onSubmit = (data: CreatePaymentRequest) => {
    createMutation.mutate(data);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" fontWeight="bold">
          Paiements
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>
          Nouveau paiement
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
            <TextField
              select
              label="Filtrer par statut"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              sx={{ minWidth: 200 }}
            >
              <MenuItem value="">Tous</MenuItem>
              {STATUS_OPTIONS.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </TextField>
          </Box>

          <DataGrid
            rows={paymentsData?.data || []}
            columns={columns}
            loading={isLoading}
            paginationMode="server"
            rowCount={paymentsData?.pageInfo?.totalElements || 0}
            pageSizeOptions={[5, 10, 25]}
            paginationModel={{ page, pageSize }}
            onPaginationModelChange={(model) => {
              setPage(model.page);
              setPageSize(model.pageSize);
            }}
            autoHeight
            disableRowSelectionOnClick
          />
        </CardContent>
      </Card>

      {/* Create Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogTitle>Nouveau paiement</DialogTitle>
          <DialogContent>
            <TextField
              fullWidth
              label="User ID"
              type="number"
              margin="normal"
              {...register('userId', { required: 'User ID requis', valueAsNumber: true })}
              error={!!errors.userId}
              helperText={errors.userId?.message}
            />
            <TextField
              fullWidth
              label="Travel ID (optionnel)"
              type="number"
              margin="normal"
              {...register('travelId', { valueAsNumber: true })}
            />
            <TextField
              fullWidth
              label="Payment Method ID"
              type="number"
              margin="normal"
              {...register('paymentMethodId', { required: 'Payment Method ID requis', valueAsNumber: true })}
              error={!!errors.paymentMethodId}
              helperText={errors.paymentMethodId?.message}
            />
            <TextField
              fullWidth
              label="Montant"
              type="number"
              margin="normal"
              {...register('amount', { required: 'Montant requis', valueAsNumber: true })}
              error={!!errors.amount}
              helperText={errors.amount?.message}
            />
            <TextField
              fullWidth
              label="Description"
              margin="normal"
              multiline
              rows={2}
              {...register('description')}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDialogOpen(false)}>Annuler</Button>
            <Button type="submit" variant="contained">
              Créer
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Refund Dialog */}
      <Dialog open={refundDialogOpen} onClose={() => setRefundDialogOpen(false)}>
        <DialogTitle>Rembourser le paiement</DialogTitle>
        <DialogContent>
          <Typography sx={{ mb: 2 }}>
            Montant original: {selectedPayment?.amount.toLocaleString()} {selectedPayment?.currency}
          </Typography>
          <TextField
            fullWidth
            label="Montant à rembourser"
            type="number"
            value={refundAmount}
            onChange={(e) => setRefundAmount(Number(e.target.value))}
            inputProps={{ max: selectedPayment?.amount }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRefundDialogOpen(false)}>Annuler</Button>
          <Button
            color="warning"
            variant="contained"
            onClick={() => selectedPayment && refundMutation.mutate({ id: selectedPayment.id, amount: refundAmount })}
          >
            Rembourser
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity={snackbar.severity}>{snackbar.message}</Alert>
      </Snackbar>
    </Box>
  );
}
